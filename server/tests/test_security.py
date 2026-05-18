"""
Tests for security functions
"""
import pytest
from datetime import timedelta
from jose import jwt

from app.core.security import (
    verify_password,
    hash_password,
    create_access_token,
    create_refresh_token
)
from app.core.config import settings


class TestPasswordHashing:
    """Tests for password hashing functions"""

    def test_hash_password(self):
        """Test password hashing"""
        password = "SecurePassword123!"
        hashed = hash_password(password)

        assert hashed != password
        assert len(hashed) > 0

    def test_verify_password_correct(self):
        """Test verifying correct password"""
        password = "SecurePassword123!"
        hashed = hash_password(password)

        assert verify_password(password, hashed) is True

    def test_verify_password_incorrect(self):
        """Test verifying incorrect password"""
        password = "SecurePassword123!"
        hashed = hash_password(password)

        assert verify_password("WrongPassword", hashed) is False

    def test_hash_different_each_time(self):
        """Test that hash is different each time (salt)"""
        password = "SecurePassword123!"
        hash1 = hash_password(password)
        hash2 = hash_password(password)

        assert hash1 != hash2
        assert verify_password(password, hash1) is True
        assert verify_password(password, hash2) is True


class TestJWTTokens:
    """Tests for JWT token functions"""

    def test_create_access_token(self):
        """Test creating access token"""
        data = {"sub": "test-user-id"}
        token = create_access_token(data=data)

        assert token is not None
        assert len(token) > 0

        # Decode and verify
        payload = jwt.decode(
            token,
            settings.SECRET_KEY,
            algorithms=[settings.ALGORITHM]
        )
        assert payload["sub"] == "test-user-id"
        assert "exp" in payload

    def test_create_refresh_token(self):
        """Test creating refresh token"""
        data = {"sub": "test-user-id"}
        token = create_refresh_token(data=data)

        assert token is not None
        assert len(token) > 0

        # Decode and verify
        payload = jwt.decode(
            token,
            settings.SECRET_KEY,
            algorithms=[settings.ALGORITHM]
        )
        assert payload["sub"] == "test-user-id"
        assert payload["type"] == "refresh"

    def test_access_token_with_custom_expiry(self):
        """Test access token with custom expiry"""
        data = {"sub": "test-user-id"}
        expires_delta = timedelta(hours=1)
        token = create_access_token(data=data, expires_delta=expires_delta)

        payload = jwt.decode(
            token,
            settings.SECRET_KEY,
            algorithms=[settings.ALGORITHM]
        )

        assert "exp" in payload

    def test_tokens_are_different(self):
        """Test that access and refresh tokens are different"""
        data = {"sub": "test-user-id"}
        access_token = create_access_token(data=data)
        refresh_token = create_refresh_token(data=data)

        assert access_token != refresh_token
