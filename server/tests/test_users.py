"""
Tests for user endpoints
"""
import pytest
from httpx import AsyncClient
from faker import Faker

fake = Faker('uk_UA')


class TestUserProfile:
    """Tests for user profile operations"""

    @pytest.mark.asyncio
    async def test_get_current_user(self, client: AsyncClient, test_user, auth_headers):
        """Test getting current user profile"""
        response = await client.get("/api/v1/users/me", headers=auth_headers)

        assert response.status_code == 200
        data = response.json()
        assert data["id"] == test_user.id
        assert data["email"] == test_user.email
        assert data["full_name"] == test_user.full_name

    @pytest.mark.asyncio
    async def test_get_current_user_unauthorized(self, client: AsyncClient):
        """Test getting profile without auth"""
        response = await client.get("/api/v1/users/me")

        assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_update_profile(self, client: AsyncClient, auth_headers):
        """Test updating user profile"""
        new_name = fake.name()
        response = await client.put("/api/v1/users/me", headers=auth_headers, json={
            "full_name": new_name,
            "avatar_url": "https://example.com/avatar.jpg"
        })

        assert response.status_code == 200
        data = response.json()
        assert data["full_name"] == new_name

    @pytest.mark.asyncio
    async def test_get_user_by_id(self, client: AsyncClient, test_user, auth_headers):
        """Test getting user by ID"""
        response = await client.get(f"/api/v1/users/{test_user.id}", headers=auth_headers)

        assert response.status_code == 200
        data = response.json()
        assert data["id"] == test_user.id

    @pytest.mark.asyncio
    async def test_get_nonexistent_user(self, client: AsyncClient, auth_headers):
        """Test getting non-existent user"""
        response = await client.get("/api/v1/users/nonexistent-id", headers=auth_headers)

        assert response.status_code == 404
