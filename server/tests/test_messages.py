"""
Tests for message endpoints
"""
import pytest
from httpx import AsyncClient
from faker import Faker

fake = Faker('uk_UA')


class TestMessageOperations:
    """Tests for message CRUD operations"""

    @pytest.mark.asyncio
    async def test_send_message(self, client: AsyncClient, test_task, auth_headers):
        """Test sending a message"""
        response = await client.post("/api/v1/messages", headers=auth_headers, json={
            "task_id": test_task.id,
            "content": "Test message content",
            "type": "text"
        })

        assert response.status_code == 200
        data = response.json()
        assert data["content"] == "Test message content"
        assert data["type"] == "text"

    @pytest.mark.asyncio
    async def test_get_messages(self, client: AsyncClient, test_task, auth_headers):
        """Test getting messages for a task"""
        # First send a message
        await client.post("/api/v1/messages", headers=auth_headers, json={
            "task_id": test_task.id,
            "content": "Message 1"
        })

        await client.post("/api/v1/messages", headers=auth_headers, json={
            "task_id": test_task.id,
            "content": "Message 2"
        })

        # Get messages
        response = await client.get(
            f"/api/v1/messages/{test_task.id}",
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) >= 2

    @pytest.mark.asyncio
    async def test_get_messages_for_nonexistent_task(self, client: AsyncClient, auth_headers):
        """Test getting messages for non-existent task"""
        response = await client.get(
            "/api/v1/messages/nonexistent-task-id",
            headers=auth_headers
        )

        assert response.status_code == 404

    @pytest.mark.asyncio
    async def test_mark_messages_as_read(self, client: AsyncClient, test_task, auth_headers):
        """Test marking messages as read"""
        # Send a message
        await client.post("/api/v1/messages", headers=auth_headers, json={
            "task_id": test_task.id,
            "content": "Unread message"
        })

        # Mark as read
        response = await client.post(
            f"/api/v1/messages/{test_task.id}/read",
            headers=auth_headers
        )

        assert response.status_code == 200

    @pytest.mark.asyncio
    async def test_send_message_to_nonexistent_task(self, client: AsyncClient, auth_headers):
        """Test sending message to non-existent task"""
        response = await client.post("/api/v1/messages", headers=auth_headers, json={
            "task_id": "nonexistent-task-id",
            "content": "Test message"
        })

        assert response.status_code == 404
