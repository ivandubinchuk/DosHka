"""
Роутер повідомлень
"""
from typing import Annotated
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload

from ....core.database import get_db
from ....core.deps import CurrentUser
from ....models import Message, Task
from ....schemas import MessageCreate, MessageResponse

router = APIRouter(prefix="/messages", tags=["Повідомлення"])


@router.get("/{task_id}", response_model=list[MessageResponse])
async def get_messages(
    task_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати повідомлення задачі"""
    # Перевіряємо чи задача існує
    task_result = await db.execute(select(Task).where(Task.id == task_id))
    task = task_result.scalar_one_or_none()
    if not task:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Задачу не знайдено"
        )

    # Виконавець може бачити тільки чат своїх задач
    if current_user.role == "executor" and task.assignee_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Немає доступу до цього чату"
        )

    result = await db.execute(
        select(Message)
        .options(selectinload(Message.sender))
        .where(Message.task_id == task_id)
        .order_by(Message.created_at)
    )
    return result.scalars().all()


@router.post("", response_model=MessageResponse)
async def send_message(
    request: MessageCreate,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Надіслати повідомлення"""
    # Перевіряємо чи задача існує
    task_result = await db.execute(select(Task).where(Task.id == request.task_id))
    task = task_result.scalar_one_or_none()
    if not task:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Задачу не знайдено"
        )

    # Виконавець може писати тільки в чат своїх задач
    if current_user.role == "executor" and task.assignee_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Немає доступу до цього чату"
        )

    message = Message(
        task_id=request.task_id,
        sender_id=current_user.id,
        content=request.content,
        type=request.type
    )
    db.add(message)
    await db.commit()
    await db.refresh(message, ["sender"])

    return message


@router.post("/{task_id}/read")
async def mark_as_read(
    task_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Позначити всі повідомлення як прочитані"""
    result = await db.execute(
        select(Message)
        .where(Message.task_id == task_id)
        .where(Message.is_read == False)
        .where(Message.sender_id != current_user.id)
    )
    messages = result.scalars().all()

    for message in messages:
        message.is_read = True

    await db.commit()

    return {"message": f"Позначено {len(messages)} повідомлень як прочитані"}
