package com.example.notification.service

import com.example.notification.domain.Notification
import com.example.notification.domain.NotificationChannel
import com.example.notification.repository.NotificationRepository
import com.example.outbox.repository.OutboxRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Optional
import java.util.UUID

@ExtendWith(MockKExtension::class)
class NotificationServiceTest {
    @MockK
    private lateinit var notificationRepository: NotificationRepository

    @MockK
    private lateinit var outboxRepository: OutboxRepository

    @InjectMockKs
    private lateinit var notificationService: NotificationService

    @Test
    fun `should send notification successfully`() {
        val sendRequest =
            SendNotificationRequest(
                recipientEmail = "test@example.com",
                subject = "Test Subject",
                content = "Test Content",
                channel = NotificationChannel.EMAIL,
            )

        val notification =
            Notification(
                recipientEmail = sendRequest.recipientEmail,
                subject = sendRequest.subject,
                content = sendRequest.content,
                channel = sendRequest.channel,
            ).apply { id = UUID.randomUUID() }

        every { notificationRepository.save(any()) } answers { firstArg() }
        every { outboxRepository.save(any()) } returns Unit

        val result = notificationService.sendNotification(sendRequest)

        assertNotNull(result)
        assertEquals(sendRequest.recipientEmail, result.recipientEmail)
        assertEquals(sendRequest.subject, result.subject)
        assertEquals(sendRequest.content, result.content)
        assertEquals(sendRequest.channel, result.channel)

        verify(exactly = 2) { notificationRepository.save(any()) }
        verify(exactly = 1) { outboxRepository.save(any()) }
    }

    @Test
    fun `should throw exception when notification fails to send`() {
        val sendRequest =
            SendNotificationRequest(
                recipientEmail = "test@example.com",
                subject = "Test Subject",
                content = "Test Content",
                channel = NotificationChannel.EMAIL,
            )

        every { notificationRepository.save(any()) } throws RuntimeException("Simulated failure")

        assertThrows<RuntimeException> { notificationService.sendNotification(sendRequest) }

        verify(exactly = 1) { notificationRepository.save(any()) }
        verify(exactly = 0) { outboxRepository.save(any()) }
    }

    @Test
    fun `should get notification by ID when exists`() {
        val notificationId = UUID.randomUUID()
        val notification =
            Notification(
                recipientEmail = "test@example.com",
                subject = "Test Subject",
                content = "Test Content",
                channel = NotificationChannel.EMAIL,
            ).apply { id = notificationId }

        every { notificationRepository.findById(notificationId) } returns Optional.of(notification)

        val result = notificationService.getNotificationById(notificationId)

        assertNotNull(result)
        assertEquals(notificationId, result?.id)
        assertEquals("test@example.com", result?.recipientEmail)
    }

    @Test
    fun `should return null when notification by ID does not exist`() {
        val notificationId = UUID.randomUUID()

        every { notificationRepository.findById(notificationId) } returns Optional.empty()

        val result = notificationService.getNotificationById(notificationId)

        assertNull(result)
    }
}
