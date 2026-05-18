"""
Роутер автентифікації
"""
from typing import Annotated
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from ....core.database import get_db
from ....core.security import (
    hash_password, verify_password,
    create_access_token, create_refresh_token,
    verify_refresh_token
)
from ....core.config import settings
from ....core.deps import CurrentUser
from ....models import User
from ....schemas import (
    LoginRequest, RegisterRequest, TokenResponse,
    RefreshTokenRequest, UserResponse
)

router = APIRouter(prefix="/auth", tags=["Автентифікація"])


@router.post("/login", response_model=TokenResponse)
async def login(
    request: LoginRequest,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Вхід користувача"""
    # Шукаємо користувача
    result = await db.execute(select(User).where(User.email == request.email))
    user = result.scalar_one_or_none()

    if not user or not verify_password(request.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Невірний email або пароль"
        )

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Користувач деактивований"
        )

    # Створюємо токени
    access_token = create_access_token(data={"sub": user.id})
    refresh_token = create_refresh_token(data={"sub": user.id})

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        expires_in=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60
    )


@router.post("/register", response_model=TokenResponse)
async def register(
    request: RegisterRequest,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Реєстрація нового користувача"""
    # Перевіряємо чи email вже зайнятий
    result = await db.execute(select(User).where(User.email == request.email))
    if result.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email вже зареєстровано"
        )

    # Створюємо користувача
    user = User(
        email=request.email,
        password_hash=hash_password(request.password),
        full_name=request.full_name,
        role=request.role,
        team_id=request.team_id
    )

    db.add(user)
    await db.commit()
    await db.refresh(user)

    # Створюємо токени
    access_token = create_access_token(data={"sub": user.id})
    refresh_token = create_refresh_token(data={"sub": user.id})

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        expires_in=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60
    )


@router.post("/register/invite", response_model=TokenResponse)
async def register_with_invite(
    request: RegisterRequest,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Реєстрація за запрошенням"""
    if not request.invite_token:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Потрібен токен запрошення"
        )

    # Перевіряємо invite_token
    from ....models import Team
    result = await db.execute(select(Team).where(Team.invite_code == request.invite_token))
    team = result.scalar_one_or_none()

    if not team:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Невірний код запрошення"
        )

    # Перевіряємо чи email вже зайнятий
    result = await db.execute(select(User).where(User.email == request.email))
    if result.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email вже зареєстровано"
        )

    # Створюємо користувача
    user = User(
        email=request.email,
        password_hash=hash_password(request.password),
        full_name=request.full_name,
        role="executor",
        team_id=team.id
    )

    db.add(user)
    await db.commit()
    await db.refresh(user)

    # Створюємо токени
    access_token = create_access_token(data={"sub": user.id})
    refresh_token = create_refresh_token(data={"sub": user.id})

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        expires_in=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60
    )


@router.post("/refresh", response_model=TokenResponse)
async def refresh_token(
    request: RefreshTokenRequest,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Оновлення токена"""
    user_id = verify_refresh_token(request.refresh_token)
    if not user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Невалідний refresh token"
        )

    # Перевіряємо чи користувач існує
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()

    if not user or not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Користувач не знайдений або деактивований"
        )

    # Створюємо нові токени
    access_token = create_access_token(data={"sub": user.id})
    new_refresh_token = create_refresh_token(data={"sub": user.id})

    return TokenResponse(
        access_token=access_token,
        refresh_token=new_refresh_token,
        expires_in=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60
    )


@router.post("/logout")
async def logout(current_user: CurrentUser):
    """Вихід користувача"""
    # В простій реалізації просто повертаємо успіх
    # В продакшні можна додати blacklist токенів
    return {"message": "Успішний вихід"}


@router.get("/me", response_model=UserResponse)
async def get_current_user_info(current_user: CurrentUser):
    """Отримати інформацію про поточного користувача"""
    return current_user
