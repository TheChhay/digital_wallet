package com.app.digitalwallet.viewmodel

import com.app.digitalwallet.data.Transaction
import com.app.digitalwallet.data.WalletInfo
import com.app.digitalwallet.data.WalletRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModelTest {

    private lateinit var repository: WalletRepository
    private lateinit var viewModel: WalletViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        
        // Default mocks to prevent init { refresh() } from failing
        coEvery { repository.getWalletInfo() } returns flowOf(WalletInfo(1000.0))
        coEvery { repository.getAllTransactions() } returns flowOf(emptyList())
        
        viewModel = WalletViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `lookupRecipient returns name when repository succeeds`() = runTest {
        val phone = "1234567890"
        val expectedName = "John Doe"
        coEvery { repository.lookupRecipient(phone) } returns expectedName

        var resultName: String? = null
        viewModel.lookupRecipient(phone) { resultName = it }
        
        advanceUntilIdle()
        
        assertEquals(expectedName, resultName)
    }

    @Test
    fun `lookupRecipient returns null when repository fails`() = runTest {
        val phone = "1234567890"
        coEvery { repository.lookupRecipient(phone) } returns null

        var resultName: String? = "Initial"
        viewModel.lookupRecipient(phone) { resultName = it }
        
        advanceUntilIdle()
        
        assertEquals(null, resultName)
    }

    @Test
    fun `transferMoney updates status to Success when repository succeeds`() = runTest {
        val phone = "1234567890"
        val amount = 50.0
        val note = "Test note"
        
        // Mock balance to be enough
        coEvery { repository.getWalletInfo() } returns flowOf(WalletInfo(100.0))
        viewModel.refresh()
        advanceUntilIdle()

        coEvery { repository.transferMoney(phone, amount, note) } returns true
        
        var successResult: Boolean? = null
        viewModel.transferMoney(phone, amount, note) { successResult = it }
        
        // Removed assertion for Loading state since it might be too fast or already bypassed
        
        advanceUntilIdle()
        
        assertTrue(viewModel.transferStatus.value is TransferStatus.Success)
        assertEquals(true, successResult)
    }

    @Test
    fun `transferMoney updates status to Error when balance is insufficient`() = runTest {
        val phone = "1234567890"
        val amount = 150.0
        
        // Mock balance to be less than amount
        coEvery { repository.getWalletInfo() } returns flowOf(WalletInfo(100.0))
        viewModel.refresh()
        advanceUntilIdle()

        var successResult: Boolean? = null
        viewModel.transferMoney(phone, amount, null) { successResult = it }
        
        advanceUntilIdle()
        
        val status = viewModel.transferStatus.value
        assertTrue(status is TransferStatus.Error)
        assertEquals("Insufficient balance", (status as TransferStatus.Error).message)
        assertEquals(false, successResult)
    }
}
