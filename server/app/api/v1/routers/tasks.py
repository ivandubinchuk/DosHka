"""
Роутер задач
"""
import csv
import io
import json
from datetime import datetime
from typing import Annotated
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from sqlalchemy.orm import selectinload

from ....core.database import get_db
from ....core.deps import CurrentUser, ManagerOnly
from ....models import Task, Column, Label, User, Message, Attachment, Board
from ....schemas import (
    TaskCreate, TaskUpdate, TaskResponse, MoveTaskRequest,
    LabelCreate, LabelResponse
)

router = APIRouter(prefix="/tasks", tags=["Задачі"])


@router.get("/{task_id}", response_model=TaskResponse)
async def get_task(
    task_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати задачу за ID"""
    result = await db.execute(
        select(Task)
        .options(
            selectinload(Task.assignee),
            selectinload(Task.creator),
            selectinload(Task.labels)
        )
        .where(Task.id == task_id)
        .where(Task.is_deleted == False)
    )
    task = result.scalar_one_or_none()

    if not task:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Задачу не знайдено"
        )

    # Виконавець може бачити тільки свої задачі
    if current_user.role == "executor" and task.assignee_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Немає доступу до цієї задачі"
        )

    # Рахуємо коментарі та вкладення
    comments_count = await db.execute(
        select(func.count(Message.id)).where(Message.task_id == task_id)
    )
    task.comments_count = comments_count.scalar() or 0

    attachments_count = await db.execute(
        select(func.count(Attachment.id)).where(Attachment.task_id == task_id)
    )
    task.attachments_count = attachments_count.scalar() or 0

    # Парсимо теги
    task.tags = json.loads(task.tags) if task.tags else []

    return task


@router.get("", response_model=list[TaskResponse])
async def get_tasks_by_column(
    column_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати задачі колонки"""
    query = (
        select(Task)
        .options(
            selectinload(Task.assignee),
            selectinload(Task.creator),
            selectinload(Task.labels)
        )
        .where(Task.column_id == column_id)
        .where(Task.is_deleted == False)
    )

    # Фільтрація по ролі:
    # - Менеджер бачить тільки задачі, які він створив
    # - Виконавець бачить тільки задачі, призначені йому
    if current_user.role == "manager":
        query = query.where(Task.creator_id == current_user.id)
    elif current_user.role == "executor":
        query = query.where(Task.assignee_id == current_user.id)

    result = await db.execute(query.order_by(Task.position))
    tasks = result.scalars().all()

    for task in tasks:
        task.tags = json.loads(task.tags) if task.tags else []
        task.comments_count = 0
        task.attachments_count = 0

    return tasks


@router.get("/me/tasks", response_model=list[TaskResponse])
async def get_my_tasks(
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати задачі поточного користувача"""
    result = await db.execute(
        select(Task)
        .options(
            selectinload(Task.assignee),
            selectinload(Task.creator),
            selectinload(Task.labels)
        )
        .where(Task.assignee_id == current_user.id)
        .where(Task.is_deleted == False)
        .order_by(Task.deadline)
    )
    tasks = result.scalars().all()

    for task in tasks:
        task.tags = json.loads(task.tags) if task.tags else []
        task.comments_count = 0
        task.attachments_count = 0

    return tasks


@router.post("", response_model=TaskResponse)
async def create_task(
    request: TaskCreate,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Створити нову задачу (тільки менеджер)"""
    # Перевіряємо колонку
    result = await db.execute(select(Column).where(Column.id == request.column_id))
    column = result.scalar_one_or_none()

    if not column:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Колонку не знайдено"
        )

    # Перевіряємо WIP ліміт
    if column.wip_limit:
        count_result = await db.execute(
            select(func.count(Task.id))
            .where(Task.column_id == column.id)
            .where(Task.is_deleted == False)
        )
        current_count = count_result.scalar() or 0
        if current_count >= column.wip_limit:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Досягнуто WIP ліміт ({column.wip_limit}) для цієї колонки"
            )

    # Визначаємо позицію
    max_pos_result = await db.execute(
        select(func.max(Task.position))
        .where(Task.column_id == request.column_id)
    )
    max_pos = max_pos_result.scalar() or 0

    # Створюємо задачу
    task = Task(
        title=request.title,
        description=request.description,
        column_id=request.column_id,
        position=max_pos + 1,
        priority=request.priority,
        assignee_id=request.assignee_id,
        creator_id=current_user.id,
        deadline=request.deadline,
        estimated_hours=request.estimated_hours,
        tags=json.dumps(request.tags)
    )
    db.add(task)
    await db.flush()

    # Додаємо мітки
    if request.label_ids:
        labels_result = await db.execute(
            select(Label).where(Label.id.in_(request.label_ids))
        )
        labels = labels_result.scalars().all()
        task.labels = labels

    await db.commit()
    await db.refresh(task, ["assignee", "creator", "labels"])

    task.tags = json.loads(task.tags) if task.tags else []
    task.comments_count = 0
    task.attachments_count = 0

    return task


@router.put("/{task_id}", response_model=TaskResponse)
async def update_task(
    task_id: str,
    request: TaskUpdate,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Оновити задачу (тільки менеджер)"""
    result = await db.execute(
        select(Task)
        .options(selectinload(Task.assignee), selectinload(Task.creator), selectinload(Task.labels))
        .where(Task.id == task_id)
        .where(Task.is_deleted == False)
    )
    task = result.scalar_one_or_none()

    if not task:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Задачу не знайдено"
        )

    # Optimistic locking - перевіряємо версію
    if request.version is not None and request.version != task.version:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Конфлікт версій. Задачу було змінено іншим користувачем.",
            headers={"X-Current-Version": str(task.version)}
        )

    if request.title is not None:
        task.title = request.title
    if request.description is not None:
        task.description = request.description
    if request.priority is not None:
        task.priority = request.priority
    if request.assignee_id is not None:
        task.assignee_id = request.assignee_id
    if request.deadline is not None:
        task.deadline = request.deadline
    if request.estimated_hours is not None:
        task.estimated_hours = request.estimated_hours
    if request.actual_hours is not None:
        task.actual_hours = request.actual_hours
    if request.tags is not None:
        task.tags = json.dumps(request.tags)
    if request.label_ids is not None:
        labels_result = await db.execute(
            select(Label).where(Label.id.in_(request.label_ids))
        )
        task.labels = labels_result.scalars().all()

    # Інкрементуємо версію
    task.version += 1

    await db.commit()
    await db.refresh(task)

    task.tags = json.loads(task.tags) if task.tags else []
    task.comments_count = 0
    task.attachments_count = 0

    return task


@router.post("/{task_id}/move", response_model=TaskResponse)
async def move_task(
    task_id: str,
    request: MoveTaskRequest,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Перемістити задачу (тільки менеджер)"""
    result = await db.execute(
        select(Task)
        .options(selectinload(Task.assignee), selectinload(Task.creator), selectinload(Task.labels))
        .where(Task.id == task_id)
        .where(Task.is_deleted == False)
    )
    task = result.scalar_one_or_none()

    if not task:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Задачу не знайдено"
        )

    # Перевіряємо нову колонку
    result = await db.execute(select(Column).where(Column.id == request.column_id))
    new_column = result.scalar_one_or_none()

    if not new_column:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Колонку не знайдено"
        )

    # Оновлюємо позицію
    old_column_id = task.column_id
    task.column_id = request.column_id
    task.position = request.position

    # Якщо переміщуємо в колонку "Завершено", ставимо completed_at
    if new_column.name.lower() in ["завершено", "done", "completed"]:
        task.completed_at = datetime.utcnow()
    elif old_column_id != request.column_id:
        task.completed_at = None

    await db.commit()
    await db.refresh(task)

    task.tags = json.loads(task.tags) if task.tags else []
    task.comments_count = 0
    task.attachments_count = 0

    return task


@router.delete("/{task_id}")
async def delete_task(
    task_id: str,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Видалити задачу (soft delete)"""
    result = await db.execute(select(Task).where(Task.id == task_id))
    task = result.scalar_one_or_none()

    if not task:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Задачу не знайдено"
        )

    task.is_deleted = True
    await db.commit()

    return {"message": "Задачу видалено"}


# ===== Експорт =====

@router.get("/export/csv")
async def export_tasks_csv(
    team_id: str,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Експортувати задачі команди в CSV (тільки менеджер)"""
    # Отримуємо всі дошки команди
    boards_result = await db.execute(
        select(Board).where(Board.team_id == team_id)
    )
    boards = boards_result.scalars().all()
    board_ids = [b.id for b in boards]

    if not board_ids:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Дошки команди не знайдено"
        )

    # Отримуємо колонки
    columns_result = await db.execute(
        select(Column).where(Column.board_id.in_(board_ids))
    )
    columns = columns_result.scalars().all()
    column_ids = [c.id for c in columns]
    columns_map = {c.id: c.name for c in columns}

    # Отримуємо задачі
    tasks_result = await db.execute(
        select(Task)
        .options(selectinload(Task.assignee), selectinload(Task.creator))
        .where(Task.column_id.in_(column_ids))
        .where(Task.is_deleted == False)
        .order_by(Task.created_at.desc())
    )
    tasks = tasks_result.scalars().all()

    # Створюємо CSV
    output = io.StringIO()
    writer = csv.writer(output)

    # Заголовки
    writer.writerow([
        "ID", "Назва", "Опис", "Колонка", "Пріоритет",
        "Виконавець", "Творець", "Дедлайн", "Оцінка (год)",
        "Фактично (год)", "Створено", "Завершено"
    ])

    # Дані
    for task in tasks:
        writer.writerow([
            task.id,
            task.title,
            task.description or "",
            columns_map.get(task.column_id, ""),
            task.priority,
            task.assignee.full_name if task.assignee else "",
            task.creator.full_name if task.creator else "",
            task.deadline.isoformat() if task.deadline else "",
            task.estimated_hours or "",
            task.actual_hours or "",
            task.created_at.isoformat() if task.created_at else "",
            task.completed_at.isoformat() if task.completed_at else ""
        ])

    output.seek(0)

    return StreamingResponse(
        iter([output.getvalue()]),
        media_type="text/csv",
        headers={
            "Content-Disposition": f"attachment; filename=tasks_{team_id}.csv"
        }
    )


# ===== Мітки =====

@router.get("/labels", response_model=list[LabelResponse])
async def get_labels(
    team_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати мітки команди"""
    result = await db.execute(
        select(Label).where(Label.team_id == team_id)
    )
    return result.scalars().all()


@router.post("/labels", response_model=LabelResponse)
async def create_label(
    request: LabelCreate,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Створити нову мітку"""
    label = Label(
        name=request.name,
        color=request.color,
        team_id=request.team_id
    )
    db.add(label)
    await db.commit()
    await db.refresh(label)
    return label


@router.delete("/labels/{label_id}")
async def delete_label(
    label_id: str,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Видалити мітку"""
    result = await db.execute(select(Label).where(Label.id == label_id))
    label = result.scalar_one_or_none()

    if not label:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Мітку не знайдено"
        )

    await db.delete(label)
    await db.commit()

    return {"message": "Мітку видалено"}
