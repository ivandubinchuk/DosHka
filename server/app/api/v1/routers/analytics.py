"""
Роутер аналітики
"""
from datetime import datetime, timedelta
from typing import Annotated
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, and_

from ....core.database import get_db
from ....core.deps import CurrentUser, ManagerOnly
from ....models import Task, Column, Board, User
from ....schemas import DashboardStats, VelocityData, CycleTimeData

router = APIRouter(prefix="/analytics", tags=["Аналітика"])


@router.get("/dashboard", response_model=DashboardStats)
async def get_dashboard_stats(
    team_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати статистику дашборду"""
    # Отримуємо всі дошки команди
    boards_result = await db.execute(
        select(Board.id).where(Board.team_id == team_id)
    )
    board_ids = [b[0] for b in boards_result.all()]

    if not board_ids:
        return DashboardStats(
            total_tasks=0,
            completed_tasks=0,
            in_progress_tasks=0,
            overdue_tasks=0,
            completion_rate=0.0,
            average_cycle_time=None
        )

    # Отримуємо всі колонки
    columns_result = await db.execute(
        select(Column).where(Column.board_id.in_(board_ids))
    )
    columns = columns_result.scalars().all()
    column_ids = [c.id for c in columns]

    # Базовий фільтр: менеджер бачить тільки свої задачі (де він creator)
    creator_filter = Task.creator_id == current_user.id if current_user.role == "manager" else True

    # Загальна кількість задач
    total_result = await db.execute(
        select(func.count(Task.id))
        .where(Task.column_id.in_(column_ids))
        .where(Task.is_deleted == False)
        .where(creator_filter)
    )
    total_tasks = total_result.scalar() or 0

    # Завершені задачі
    completed_columns = [c.id for c in columns if c.name.lower() in ["завершено", "done", "completed"]]
    completed_result = await db.execute(
        select(func.count(Task.id))
        .where(Task.column_id.in_(completed_columns))
        .where(Task.is_deleted == False)
        .where(creator_filter)
    )
    completed_tasks = completed_result.scalar() or 0

    # В роботі
    in_progress_columns = [c.id for c in columns if c.name.lower() in ["в роботі", "in progress", "doing"]]
    in_progress_result = await db.execute(
        select(func.count(Task.id))
        .where(Task.column_id.in_(in_progress_columns))
        .where(Task.is_deleted == False)
        .where(creator_filter)
    )
    in_progress_tasks = in_progress_result.scalar() or 0

    # Прострочені
    now = datetime.utcnow()
    overdue_result = await db.execute(
        select(func.count(Task.id))
        .where(Task.column_id.in_(column_ids))
        .where(Task.is_deleted == False)
        .where(Task.deadline < now)
        .where(Task.completed_at == None)
        .where(creator_filter)
    )
    overdue_tasks = overdue_result.scalar() or 0

    # Середній час виконання
    cycle_time_result = await db.execute(
        select(func.avg(func.julianday(Task.completed_at) - func.julianday(Task.created_at)))
        .where(Task.column_id.in_(column_ids))
        .where(Task.completed_at != None)
        .where(creator_filter)
    )
    avg_days = cycle_time_result.scalar()
    average_cycle_time = int(avg_days * 24 * 60 * 60 * 1000) if avg_days else None  # в мілісекундах

    completion_rate = (completed_tasks / total_tasks * 100) if total_tasks > 0 else 0.0

    return DashboardStats(
        total_tasks=total_tasks,
        completed_tasks=completed_tasks,
        in_progress_tasks=in_progress_tasks,
        overdue_tasks=overdue_tasks,
        completion_rate=round(completion_rate, 2),
        average_cycle_time=average_cycle_time
    )


@router.get("/velocity", response_model=list[VelocityData])
async def get_velocity(
    team_id: str,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)],
    weeks: int = 8
):
    """Отримати velocity команди за тижні"""
    # Отримуємо колонки команди
    boards_result = await db.execute(
        select(Board.id).where(Board.team_id == team_id)
    )
    board_ids = [b[0] for b in boards_result.all()]

    columns_result = await db.execute(
        select(Column.id).where(Column.board_id.in_(board_ids))
    )
    column_ids = [c[0] for c in columns_result.all()]

    velocity_data = []
    now = datetime.utcnow()

    for week_offset in range(weeks - 1, -1, -1):
        week_start = now - timedelta(weeks=week_offset + 1)
        week_end = now - timedelta(weeks=week_offset)

        # Рахуємо завершені задачі за тиждень (тільки свої для менеджера)
        completed_result = await db.execute(
            select(func.count(Task.id))
            .where(Task.column_id.in_(column_ids))
            .where(Task.completed_at >= week_start)
            .where(Task.completed_at < week_end)
            .where(Task.creator_id == current_user.id)
        )
        completed = completed_result.scalar() or 0

        velocity_data.append(VelocityData(
            week_number=week_start.isocalendar()[1],
            year=week_start.year,
            completed_tasks=completed,
            start_date=week_start.date(),
            end_date=week_end.date()
        ))

    return velocity_data


@router.get("/workload")
async def get_workload(
    team_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати навантаження виконавців команди"""
    # Отримуємо учасників команди
    users_result = await db.execute(
        select(User)
        .where(User.team_id == team_id)
        .where(User.is_active == True)
        .where(User.role == "executor")
    )
    users = users_result.scalars().all()

    # Отримуємо колонки команди
    boards_result = await db.execute(
        select(Board.id).where(Board.team_id == team_id)
    )
    board_ids = [b[0] for b in boards_result.all()]

    columns_result = await db.execute(
        select(Column).where(Column.board_id.in_(board_ids))
    )
    columns = columns_result.scalars().all()

    # Колонки активних задач (не Backlog і не Done)
    active_column_ids = [
        c.id for c in columns
        if c.name.lower() not in ["backlog", "завершено", "done", "completed"]
    ]

    # Колонки Done
    done_column_ids = [
        c.id for c in columns
        if c.name.lower() in ["завершено", "done", "completed"]
    ]

    all_column_ids = [c.id for c in columns]

    workload_data = []

    # Фільтр по творцю задачі для менеджера
    creator_filter = Task.creator_id == current_user.id if current_user.role == "manager" else True

    for user in users:
        # Активні задачі (In Progress, To Do)
        active_result = await db.execute(
            select(func.count(Task.id))
            .where(Task.column_id.in_(active_column_ids))
            .where(Task.assignee_id == user.id)
            .where(Task.is_deleted == False)
            .where(creator_filter)
        )
        active_tasks = active_result.scalar() or 0

        # Завершені задачі
        completed_result = await db.execute(
            select(func.count(Task.id))
            .where(Task.column_id.in_(done_column_ids))
            .where(Task.assignee_id == user.id)
            .where(Task.is_deleted == False)
            .where(creator_filter)
        )
        completed_tasks = completed_result.scalar() or 0

        # Прострочені активні задачі
        now = datetime.utcnow()
        overdue_result = await db.execute(
            select(func.count(Task.id))
            .where(Task.column_id.in_(active_column_ids))
            .where(Task.assignee_id == user.id)
            .where(Task.is_deleted == False)
            .where(Task.deadline < now)
            .where(creator_filter)
        )
        overdue_tasks = overdue_result.scalar() or 0

        workload_data.append({
            "user_id": user.id,
            "user_name": user.full_name,
            "active_tasks": active_tasks,
            "completed_tasks": completed_tasks,
            "overdue_tasks": overdue_tasks
        })

    # Сортуємо по кількості активних задач
    workload_data.sort(key=lambda x: x["active_tasks"], reverse=True)

    return workload_data


@router.get("/efficiency")
async def get_efficiency(
    team_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати метрики ефективності команди"""
    # Отримуємо колонки команди
    boards_result = await db.execute(
        select(Board.id).where(Board.team_id == team_id)
    )
    board_ids = [b[0] for b in boards_result.all()]

    columns_result = await db.execute(
        select(Column).where(Column.board_id.in_(board_ids))
    )
    columns = columns_result.scalars().all()

    done_column_ids = [
        c.id for c in columns
        if c.name.lower() in ["завершено", "done", "completed"]
    ]
    all_column_ids = [c.id for c in columns]

    now = datetime.utcnow()

    # Фільтр по творцю для менеджера
    creator_filter = Task.creator_id == current_user.id if current_user.role == "manager" else True

    # Завершені вчасно (до дедлайну)
    on_time_result = await db.execute(
        select(func.count(Task.id))
        .where(Task.column_id.in_(done_column_ids))
        .where(Task.is_deleted == False)
        .where(Task.deadline != None)
        .where(Task.completed_at != None)
        .where(Task.completed_at <= Task.deadline)
        .where(creator_filter)
    )
    on_time = on_time_result.scalar() or 0

    # Всього завершених з дедлайном
    with_deadline_result = await db.execute(
        select(func.count(Task.id))
        .where(Task.column_id.in_(done_column_ids))
        .where(Task.is_deleted == False)
        .where(Task.deadline != None)
        .where(Task.completed_at != None)
        .where(creator_filter)
    )
    with_deadline = with_deadline_result.scalar() or 0

    # Точність оцінок (порівняння estimated vs actual)
    estimation_result = await db.execute(
        select(
            func.sum(Task.estimated_hours),
            func.sum(Task.actual_hours),
            func.count(Task.id)
        )
        .where(Task.column_id.in_(done_column_ids))
        .where(Task.is_deleted == False)
        .where(Task.estimated_hours != None)
        .where(Task.actual_hours != None)
        .where(creator_filter)
    )
    est_row = estimation_result.one()
    total_estimated, total_actual, estimation_count = est_row

    # Середній час у кожному статусі (по колонках)
    status_distribution = []
    for col in columns:
        count_result = await db.execute(
            select(func.count(Task.id))
            .where(Task.column_id == col.id)
            .where(Task.is_deleted == False)
            .where(creator_filter)
        )
        count = count_result.scalar() or 0
        status_distribution.append({
            "column_name": col.name,
            "task_count": count
        })

    return {
        "on_time_completion_rate": round((on_time / with_deadline * 100), 1) if with_deadline > 0 else 0,
        "on_time_tasks": on_time,
        "total_with_deadline": with_deadline,
        "estimation_accuracy": round((total_estimated / total_actual * 100), 1) if total_actual and total_actual > 0 else None,
        "total_estimated_hours": total_estimated or 0,
        "total_actual_hours": total_actual or 0,
        "tasks_with_estimation": estimation_count or 0,
        "status_distribution": status_distribution
    }


@router.get("/cycle-time", response_model=list[CycleTimeData])
async def get_cycle_time(
    team_id: str,
    current_user: ManagerOnly,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати cycle time по виконавцях"""
    # Отримуємо учасників команди
    users_result = await db.execute(
        select(User)
        .where(User.team_id == team_id)
        .where(User.is_active == True)
    )
    users = users_result.scalars().all()

    # Отримуємо колонки команди
    boards_result = await db.execute(
        select(Board.id).where(Board.team_id == team_id)
    )
    board_ids = [b[0] for b in boards_result.all()]

    columns_result = await db.execute(
        select(Column.id).where(Column.board_id.in_(board_ids))
    )
    column_ids = [c[0] for c in columns_result.all()]

    cycle_time_data = []

    for user in users:
        # Середній час виконання для користувача (тільки задачі цього менеджера)
        result = await db.execute(
            select(
                func.avg(func.julianday(Task.completed_at) - func.julianday(Task.created_at)),
                func.count(Task.id)
            )
            .where(Task.column_id.in_(column_ids))
            .where(Task.assignee_id == user.id)
            .where(Task.completed_at != None)
            .where(Task.creator_id == current_user.id)
        )
        row = result.one()
        avg_days, count = row

        if count > 0:
            cycle_time_data.append(CycleTimeData(
                user_id=user.id,
                user_name=user.full_name,
                average_cycle_time_hours=round(avg_days * 24, 1) if avg_days else 0,
                tasks_completed=count
            ))

    return cycle_time_data
