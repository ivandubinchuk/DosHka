"""
API v1
"""
from fastapi import APIRouter
from .routers import (
    auth_router, users_router, teams_router,
    boards_router, tasks_router, messages_router,
    attachments_router, analytics_router
)

api_router = APIRouter(prefix="/api/v1")

api_router.include_router(auth_router)
api_router.include_router(users_router)
api_router.include_router(teams_router)
api_router.include_router(boards_router)
api_router.include_router(tasks_router)
api_router.include_router(messages_router)
api_router.include_router(attachments_router)
api_router.include_router(analytics_router)
