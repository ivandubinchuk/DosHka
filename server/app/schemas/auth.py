"""
Схеми для автентифікації
"""
from pydantic import BaseModel, EmailStr, Field


class LoginRequest(BaseModel):
    """Запит на вхід"""
    email: EmailStr
    password: str = Field(min_length=6)


class RegisterRequest(BaseModel):
    """Запит на реєстрацію"""
    email: EmailStr
    password: str = Field(min_length=6)
    full_name: str = Field(min_length=2, max_length=255)
    role: str = Field(default="executor", pattern="^(manager|executor)$")
    team_id: str | None = None
    invite_token: str | None = None


class TokenResponse(BaseModel):
    """Відповідь з токенами"""
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int  # секунди


class RefreshTokenRequest(BaseModel):
    """Запит на оновлення токена"""
    refresh_token: str


class PasswordResetRequest(BaseModel):
    """Запит на скидання пароля"""
    email: EmailStr


class PasswordChangeRequest(BaseModel):
    """Запит на зміну пароля"""
    current_password: str
    new_password: str = Field(min_length=6)
