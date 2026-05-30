"""
Роутер команд
"""
import uuid
import secrets
from datetime import datetime, timedelta
from typing import Annotated
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload
import segno
import io
import base64

from ....core.database import get_db
from ....core.deps import CurrentUser, ManagerOnly
from ....models import Team, User, Board, Column
from ....schemas import (
    TeamCreate, TeamUpdate, TeamResponse, TeamWithMembers,
    UserBrief, InviteQRResponse
)

router = APIRouter(prefix="/teams", tags=["Команди"])


@router.get("/{team_id}", response_model=TeamWithMembers)
async def get_team(
    team_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати команду за ID"""
    result = await db.execute(
        select(Team)
        .options(selectinload(Team.members))
        .where(Team.id == team_id)
    )
    team = result.scalar_one_or_none()

    if not team:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Команду не знайдено"
        )

    return team


@router.post("", response_model=TeamResponse)
async def create_team(
    request: TeamCreate,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Створити нову команду"""
    # Тільки менеджер може створювати команди
    if current_user.role != "manager":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Тільки менеджер може створювати команди"
        )

    # Генеруємо invite code
    invite_code = secrets.token_urlsafe(16)

    # Створюємо команду
    team = Team(
        name=request.name,
        description=request.description,
        manager_id=current_user.id,
        invite_code=invite_code
    )
    db.add(team)
    await db.flush()

    # Додаємо менеджера до команди
    current_user.team_id = team.id

    # Створюємо дефолтну дошку
    board = Board(
        name="Основна дошка",
        team_id=team.id,
        is_default=True
    )
    db.add(board)
    await db.flush()

    # Створюємо стандартні колонки
    default_columns = [
        ("Очікує", 0, None),
        ("В роботі", 1, 5),
        ("На перевірці", 2, 3),
        ("Завершено", 3, None)
    ]
    for name, position, wip_limit in default_columns:
        column = Column(
            name=name,
            board_id=board.id,
            position=position,
            wip_limit=wip_limit
        )
        db.add(column)

    await db.commit()
    await db.refresh(team)

    return team


@router.put("/{team_id}", response_model=TeamResponse)
async def update_team(
    team_id: str,
    request: TeamUpdate,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Оновити команду"""
    result = await db.execute(select(Team).where(Team.id == team_id))
    team = result.scalar_one_or_none()

    if not team:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Команду не знайдено"
        )

    if team.manager_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Тільки менеджер команди може її редагувати"
        )

    if request.name is not None:
        team.name = request.name
    if request.description is not None:
        team.description = request.description

    await db.commit()
    await db.refresh(team)

    return team


@router.get("/{team_id}/members", response_model=list[UserBrief])
async def get_team_members(
    team_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати учасників команди"""
    result = await db.execute(
        select(User)
        .where(User.team_id == team_id)
        .where(User.is_active == True)
    )
    members = result.scalars().all()
    return members

@router.post("/join")
async def join_team(
        invite_code: str,
        current_user: CurrentUser,
        db: Annotated[AsyncSession, Depends(get_db)]
):
    """Приєднатися до команди по інвайт-коду"""

    result = await db.execute(
        select(Team).where(Team.invite_code == invite_code)
    )
    team = result.scalar_one_or_none()

    if not team:
        raise HTTPException(status_code=404, detail="Невірний код")

    # додаємо користувача в команду
    current_user.team_id = team.id

    await db.commit()

    return {
        "status": "ok",
        "team_id": team.id,
        "team_name": team.name
    }
@router.post("/add-by-email")
async def add_by_email(
        email: str,
        current_user: ManagerOnly,
        db: Annotated[AsyncSession, Depends(get_db)]
):
    """Додати користувача в команду по email"""

    # знайти користувача
    result = await db.execute(
        select(User).where(User.email == email)
    )
    user = result.scalar_one_or_none()

    if not user:
        raise HTTPException(
            status_code=404,
            detail="Користувача не знайдено"
        )

    # перевірка що менеджер має команду
    if not current_user.team_id:
        raise HTTPException(
            status_code=400,
            detail="У менеджера немає команди"
        )

    # перевірка чи вже в команді
    if user.team_id == current_user.team_id:
        return {"status": "already_in_team"}

    # додаємо в команду
    user.team_id = current_user.team_id

    await db.commit()

    return {
        "status": "ok",
        "message": f"{user.email} додано в команду"
    }

@router.post("/invite/qr", response_model=InviteQRResponse)
async def generate_invite_qr(
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Згенерувати QR-код запрошення"""
    if not current_user.team_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Користувач не належить до команди"
        )

    # Отримуємо команду
    result = await db.execute(select(Team).where(Team.id == current_user.team_id))
    team = result.scalar_one_or_none()

    if not team:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Команду не знайдено"
        )

    if team.manager_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Тільки менеджер може генерувати запрошення"
        )

    # Генеруємо новий invite code
    invite_code = secrets.token_urlsafe(16)
    team.invite_code = invite_code
    await db.commit()

    # Генеруємо QR-код
    qr_data = f"doshka://invite?token={invite_code}"
    qr = segno.make(qr_data)

    # Конвертуємо в base64
    buffer = io.BytesIO()
    qr.save(buffer, kind="png", scale=10)
    buffer.seek(0)
    qr_base64 = base64.b64encode(buffer.getvalue()).decode()

    return InviteQRResponse(
        qr_code=f"data:image/png;base64,{qr_base64}",
        invite_token=invite_code,
        expires_at=datetime.utcnow() + timedelta(hours=24)
    )
