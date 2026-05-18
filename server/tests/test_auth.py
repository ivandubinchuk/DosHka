"""
Tests for authentication endpoints
"""
import pytest
from httpx import AsyncClient
from faker import Faker

fake = Faker('uk_UA')


class TestAuthRegister:
    """Tests for user registration"""

    @pytest.mark.asyncio
    async def test_register_success(self, client: AsyncClient):
        """Test successful user registration"""
        response = await client.post("/api/v1/auth/register", json={
            "email": fake.email(),
            "password": "SecurePass123!",
            "full_name": fake.name()
        })

        assert response.status_code == 200
        data = response.json()
        assert "access_token" in data
        assert "refresh_token" in data
        assert data["token_type"] == "bearer"

    @pytest.mark.asyncio
    async def test_register_duplicate_email(self, client: AsyncClient, test_user):
        """Test registration with existing email"""
        response = await client.post("/api/v1/auth/register", json={
            "email": test_user.email,
            "password": "SecurePass123!",
            "full_name": fake.name()
        })

        assert response.status_code == 400
        assert "вже зареєстрований" in response.json()["detail"]

    @pytest.mark.asyncio
    async def test_register_weak_password(self, client: AsyncClient):
        """Test registration with weak password"""
        response = await client.post("/api/v1/auth/register", json={
            "email": fake.email(),
            "password": "weak",
            "full_name": fake.name()
        })

        assert response.status_code == 422

    @pytest.mark.asyncio
    async def test_register_invalid_email(self, client: AsyncClient):
        """Test registration with invalid email format"""
        response = await client.post("/api/v1/auth/register", json={
            "email": "not-an-email",
            "password": "SecurePass123!",
            "full_name": fake.name()
        })

        assert response.status_code == 422


class TestAuthLogin:
    """Tests for user login"""

    @pytest.mark.asyncio
    async def test_login_success(self, client: AsyncClient, test_user):
        """Test successful login"""
        response = await client.post("/api/v1/auth/login", json={
            "email": test_user.email,
            "password": "TestPassword123!"
        })

        assert response.status_code == 200
        data = response.json()
        assert "access_token" in data
        assert "refresh_token" in data

    @pytest.mark.asyncio
    async def test_login_wrong_password(self, client: AsyncClient, test_user):
        """Test login with wrong password"""
        response = await client.post("/api/v1/auth/login", json={
            "email": test_user.email,
            "password": "WrongPassword123!"
        })

        assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_login_nonexistent_user(self, client: AsyncClient):
        """Test login with non-existent email"""
        response = await client.post("/api/v1/auth/login", json={
            "email": "nonexistent@example.com",
            "password": "AnyPassword123!"
        })

        assert response.status_code == 401


class TestAuthToken:
    """Tests for token operations"""

    @pytest.mark.asyncio
    async def test_refresh_token_success(self, client: AsyncClient, test_user):
        """Test successful token refresh"""
        # First login to get tokens
        login_response = await client.post("/api/v1/auth/login", json={
            "email": test_user.email,
            "password": "TestPassword123!"
        })
        refresh_token = login_response.json()["refresh_token"]

        # Refresh token
        response = await client.post("/api/v1/auth/refresh", json={
            "refresh_token": refresh_token
        })

        assert response.status_code == 200
        data = response.json()
        assert "access_token" in data

    @pytest.mark.asyncio
    async def test_refresh_token_invalid(self, client: AsyncClient):
        """Test refresh with invalid token"""
        response = await client.post("/api/v1/auth/refresh", json={
            "refresh_token": "invalid-token"
        })

        assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_logout_success(self, client: AsyncClient, auth_headers):
        """Test successful logout"""
        response = await client.post("/api/v1/auth/logout", headers=auth_headers)

        assert response.status_code == 200

    @pytest.mark.asyncio
    async def test_logout_without_auth(self, client: AsyncClient):
        """Test logout without authentication"""
        response = await client.post("/api/v1/auth/logout")

        assert response.status_code == 401
