"""
API роутери v1
"""
from .auth import router as auth_router
from .users import router as users_router
from .teams import router as teams_router
from .boards import router as boards_router
from .tasks import router as tasks_router
from .messages import router as messages_router
from .attachments import router as attachments_router
from .analytics import router as analytics_router

__all__ = [
    "auth_router",
    "users_router",
    "teams_router",
    "boards_router",
    "tasks_router",
    "messages_router",
    "attachments_router",
    "analytics_router"
]
