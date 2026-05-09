package utils

import (
	"net/http"

	"digital_wallet_api/internal/dto"
	"github.com/gin-gonic/gin"
)

func Success(c *gin.Context, status int, message string, data interface{}) {
	c.JSON(status, dto.APIResponse{Success: true, Message: message, Data: data})
}

func Error(c *gin.Context, status int, message string, errors interface{}) {
	c.JSON(status, dto.APIResponse{Success: false, Message: message, Errors: errors})
}

func BadRequest(c *gin.Context, err error) {
	Error(c, http.StatusBadRequest, "Invalid request", err.Error())
}
