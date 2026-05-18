"""
Схеми для аналітики
"""
from datetime import date
from pydantic import BaseModel


class DashboardStats(BaseModel):
    """Статистика дашборду"""
    total_tasks: int
    completed_tasks: int
    in_progress_tasks: int
    overdue_tasks: int
    completion_rate: float
    average_cycle_time: int | None = None  # мілісекунди


class VelocityData(BaseModel):
    """Дані velocity"""
    week_number: int
    year: int
    completed_tasks: int
    start_date: date
    end_date: date


class CycleTimeData(BaseModel):
    """Дані cycle time по виконавцю"""
    user_id: str
    user_name: str
    average_cycle_time_hours: float
    tasks_completed: int
