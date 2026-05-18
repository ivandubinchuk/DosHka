"""
Роутер дошок та колонок
"""
from typing import Annotated
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from sqlalchemy.orm import selectinload

from ....core.database import get_db
from ....core.deps import CurrentUser, ManagerOnly
from ....models import Board, Column, Task
from ....schemas import (
    BoardCreate, BoardUpdate, BoardResponse, BoardWithColumns,
    ColumnCreate, ColumnUpdate, ColumnResponse
)

router = APIRouter(prefix="/boards", tags=["Дошки"])


@router.get("/{board_id}", response_model=BoardWithColumns)
async def get_board(
    board_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати дошку за ID"""
    result = await db.execute(
        select(Board)
        .options(selectinload(Board.columns))
        .where(Board.id == board_id)
    )
    board = result.scalar_one_or_none()

    if not board:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Дошку не знайдено"
        )

    # Рахуємо задачі для кожної колонки
    for column in board.columns:
        count_result = await db.execute(
            select(func.count(Task.id))
            .where(Task.column_id == column.id)
            .where(Task.is_deleted == False)
        )
        column.task_count = count_result.scalar() or 0

    return board


@router.get("", response_model=list[BoardResponse])
async def get_boards_by_team(
    team_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати дошки команди"""
    result = await db.execute(
        select(Board).where(Board.team_id == team_id)
    )
    boards = result.scalars().all()
    return boards


@router.post("", response_model=BoardResponse)
async def create_board(
    request: BoardCreate,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Створити нову дошку"""
    board = Board(
        name=request.name,
        description=request.description,
        team_id=request.team_id
    )
    db.add(board)
    await db.commit()
    await db.refresh(board)
    return board


@router.put("/{board_id}", response_model=BoardResponse)
async def update_board(
    board_id: str,
    request: BoardUpdate,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Оновити дошку"""
    result = await db.execute(select(Board).where(Board.id == board_id))
    board = result.scalar_one_or_none()

    if not board:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Дошку не знайдено"
        )

    if request.name is not None:
        board.name = request.name
    if request.description is not None:
        board.description = request.description

    await db.commit()
    await db.refresh(board)
    return board


@router.delete("/{board_id}")
async def delete_board(
    board_id: str,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Видалити дошку"""
    result = await db.execute(select(Board).where(Board.id == board_id))
    board = result.scalar_one_or_none()

    if not board:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Дошку не знайдено"
        )

    if board.is_default:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Неможливо видалити дефолтну дошку"
        )

    await db.delete(board)
    await db.commit()

    return {"message": "Дошку видалено"}


# ===== Колонки =====

@router.get("/{board_id}/columns", response_model=list[ColumnResponse])
async def get_columns(
    board_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати колонки дошки"""
    result = await db.execute(
        select(Column)
        .where(Column.board_id == board_id)
        .order_by(Column.position)
    )
    columns = result.scalars().all()

    # Рахуємо задачі для кожної колонки
    for column in columns:
        count_result = await db.execute(
            select(func.count(Task.id))
            .where(Task.column_id == column.id)
            .where(Task.is_deleted == False)
        )
        column.task_count = count_result.scalar() or 0

    return columns


@router.post("/columns", response_model=ColumnResponse)
async def create_column(
    request: ColumnCreate,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Створити нову колонку"""
    column = Column(
        name=request.name,
        board_id=request.board_id,
        position=request.position,
        wip_limit=request.wip_limit,
        color=request.color
    )
    db.add(column)
    await db.commit()
    await db.refresh(column)
    column.task_count = 0
    return column


@router.put("/columns/{column_id}", response_model=ColumnResponse)
async def update_column(
    column_id: str,
    request: ColumnUpdate,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Оновити колонку"""
    result = await db.execute(select(Column).where(Column.id == column_id))
    column = result.scalar_one_or_none()

    if not column:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Колонку не знайдено"
        )

    if request.name is not None:
        column.name = request.name
    if request.position is not None:
        column.position = request.position
    if request.wip_limit is not None:
        column.wip_limit = request.wip_limit
    if request.color is not None:
        column.color = request.color

    await db.commit()
    await db.refresh(column)

    # Рахуємо задачі
    count_result = await db.execute(
        select(func.count(Task.id))
        .where(Task.column_id == column.id)
        .where(Task.is_deleted == False)
    )
    column.task_count = count_result.scalar() or 0

    return column


@router.delete("/columns/{column_id}")
async def delete_column(
    column_id: str,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Видалити колонку"""
    result = await db.execute(select(Column).where(Column.id == column_id))
    column = result.scalar_one_or_none()

    if not column:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Колонку не знайдено"
        )

    await db.delete(column)
    await db.commit()

    return {"message": "Колонку видалено"}
