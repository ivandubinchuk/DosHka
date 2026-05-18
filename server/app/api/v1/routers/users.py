"""
Роутер користувачів
"""
from typing import Annotated
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from ....core.database import get_db
from ....core.deps import CurrentUser
from ....models import User
from ....schemas import UserResponse, UserUpdate

router = APIRouter(prefix="/users", tags=["Користувачі"])


@router.get("/me", response_model=UserResponse)
async def get_current_user(current_user: CurrentUser):
    """Отримати поточного користувача"""
    return current_user


@router.put("/me", response_model=UserResponse)
async def update_current_user(
    request: UserUpdate,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Оновити профіль поточного користувача"""
    if request.full_name is not None:
        current_user.full_name = request.full_name
    if request.avatar_url is not None:
        current_user.avatar_url = request.avatar_url

    await db.commit()
    await db.refresh(current_user)

    return current_user


@router.get("/{user_id}", response_model=UserResponse)
async def get_user(
    user_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати користувача за ID"""
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Користувача не знайдено"
        )

    return user
