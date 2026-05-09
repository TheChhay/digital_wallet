package storage

import (
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"net/url"
	"os"
	"path"
	"path/filepath"
	"strings"
	"time"

	"digital_wallet_api/internal/config"
	"github.com/google/uuid"
)

const MaxImageSize = 5 << 20

var allowedImageTypes = map[string]string{
	"image/jpeg": ".jpg",
	"image/png":  ".png",
	"image/webp": ".webp",
}

type Store interface {
	UploadImage(ctx context.Context, file multipart.File, header *multipart.FileHeader, folder string) (string, error)
}

func New(cfg *config.Config) Store {
	if cfg.AppEnv == "production" {
		return &R2Store{
			accessKeyID:   cfg.R2AccessKeyID,
			secretKey:     cfg.R2SecretKey,
			bucket:        cfg.R2Bucket,
			endpoint:      strings.TrimRight(cfg.R2Endpoint, "/"),
			publicBaseURL: strings.TrimRight(cfg.R2PublicBaseURL, "/"),
			region:        cfg.R2Region,
			client:        http.DefaultClient,
		}
	}
	return &LocalStore{
		root:    cfg.UploadDir,
		baseURL: "/uploads",
	}
}

func readImage(file multipart.File, header *multipart.FileHeader) ([]byte, string, error) {
	if header.Size > MaxImageSize {
		return nil, "", fmt.Errorf("image must be 5MB or smaller")
	}
	data, err := io.ReadAll(io.LimitReader(file, MaxImageSize+1))
	if err != nil {
		return nil, "", err
	}
	if len(data) > MaxImageSize {
		return nil, "", fmt.Errorf("image must be 5MB or smaller")
	}
	contentType := http.DetectContentType(data)
	ext, ok := allowedImageTypes[contentType]
	if !ok {
		return nil, "", fmt.Errorf("only jpeg, png, and webp images are allowed")
	}
	return data, ext, nil
}

func cleanFolder(folder string) string {
	folder = strings.Trim(strings.ReplaceAll(folder, "\\", "/"), "/")
	if folder == "" || strings.Contains(folder, "..") {
		return "images"
	}
	return folder
}

func objectKey(folder, ext string) string {
	return path.Join(cleanFolder(folder), time.Now().UTC().Format("20060102"), uuid.NewString()+ext)
}

type LocalStore struct {
	root    string
	baseURL string
}

func (s *LocalStore) UploadImage(_ context.Context, file multipart.File, header *multipart.FileHeader, folder string) (string, error) {
	data, ext, err := readImage(file, header)
	if err != nil {
		return "", err
	}
	key := objectKey(folder, ext)
	target := filepath.Join(s.root, filepath.FromSlash(key))
	if err := os.MkdirAll(filepath.Dir(target), 0755); err != nil {
		return "", err
	}
	if err := os.WriteFile(target, data, 0644); err != nil {
		return "", err
	}
	return s.baseURL + "/" + key, nil
}

type R2Store struct {
	accessKeyID   string
	secretKey     string
	bucket        string
	endpoint      string
	publicBaseURL string
	region        string
	client        *http.Client
}

func (s *R2Store) UploadImage(ctx context.Context, file multipart.File, header *multipart.FileHeader, folder string) (string, error) {
	data, ext, err := readImage(file, header)
	if err != nil {
		return "", err
	}
	key := objectKey(folder, ext)
	objectURL := s.endpoint + "/" + s.bucket + "/" + key
	req, err := http.NewRequestWithContext(ctx, http.MethodPut, objectURL, bytes.NewReader(data))
	if err != nil {
		return "", err
	}
	contentType := http.DetectContentType(data)
	hash := sha256.Sum256(data)
	payloadHash := hex.EncodeToString(hash[:])
	req.Header.Set("Content-Type", contentType)
	req.Header.Set("X-Amz-Content-Sha256", payloadHash)
	req.Header.Set("X-Amz-Date", time.Now().UTC().Format("20060102T150405Z"))
	signRequest(req, s.accessKeyID, s.secretKey, s.region, payloadHash)

	resp, err := s.client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		body, _ := io.ReadAll(io.LimitReader(resp.Body, 1024))
		return "", fmt.Errorf("r2 upload failed: %s %s", resp.Status, strings.TrimSpace(string(body)))
	}
	return s.publicBaseURL + "/" + key, nil
}

func signRequest(req *http.Request, accessKeyID, secretKey, region, payloadHash string) {
	now := time.Now().UTC()
	amzDate := req.Header.Get("X-Amz-Date")
	if amzDate == "" {
		amzDate = now.Format("20060102T150405Z")
		req.Header.Set("X-Amz-Date", amzDate)
	}
	dateStamp := amzDate[:8]
	req.Header.Set("Host", req.URL.Host)

	canonicalURI := req.URL.EscapedPath()
	canonicalQuery := canonicalQueryString(req.URL.Query())
	canonicalHeaders := "content-type:" + strings.TrimSpace(req.Header.Get("Content-Type")) + "\n" +
		"host:" + req.URL.Host + "\n" +
		"x-amz-content-sha256:" + payloadHash + "\n" +
		"x-amz-date:" + amzDate + "\n"
	signedHeaders := "content-type;host;x-amz-content-sha256;x-amz-date"
	canonicalRequest := strings.Join([]string{
		req.Method,
		canonicalURI,
		canonicalQuery,
		canonicalHeaders,
		signedHeaders,
		payloadHash,
	}, "\n")

	credentialScope := dateStamp + "/" + region + "/s3/aws4_request"
	requestHash := sha256.Sum256([]byte(canonicalRequest))
	stringToSign := strings.Join([]string{
		"AWS4-HMAC-SHA256",
		amzDate,
		credentialScope,
		hex.EncodeToString(requestHash[:]),
	}, "\n")
	signingKey := signatureKey(secretKey, dateStamp, region, "s3")
	signature := hex.EncodeToString(hmacSHA256(signingKey, stringToSign))
	req.Header.Set("Authorization", "AWS4-HMAC-SHA256 Credential="+accessKeyID+"/"+credentialScope+", SignedHeaders="+signedHeaders+", Signature="+signature)
}

func canonicalQueryString(values url.Values) string {
	if len(values) == 0 {
		return ""
	}
	return values.Encode()
}

func signatureKey(secret, dateStamp, region, service string) []byte {
	kDate := hmacSHA256([]byte("AWS4"+secret), dateStamp)
	kRegion := hmacSHA256(kDate, region)
	kService := hmacSHA256(kRegion, service)
	return hmacSHA256(kService, "aws4_request")
}

func hmacSHA256(key []byte, data string) []byte {
	mac := hmac.New(sha256.New, key)
	mac.Write([]byte(data))
	return mac.Sum(nil)
}
