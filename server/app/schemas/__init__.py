"""
Pydantic схеми
"""
from .auth import (
    LoginRequest, RegisterRequest, TokenResponse,
    RefreshTokenRequest, PasswordResetRequest, PasswordChangeRequest
)
from .user import UserCreate, UserUpdate, UserResponse, UserBrief
from .team import TeamCreate, TeamUpdate, TeamResponse, TeamWithMembers, InviteQRResponse
from .board import (
    BoardCreate, BoardUpdate, BoardResponse, BoardWithColumns,
    ColumnCreate, ColumnUpdate, ColumnResponse
)
from .task import (
    TaskCreate, TaskUpdate, TaskResponse, TaskBrief, MoveTaskRequest,
    LabelCreate, LabelResponse
)
from .message import MessageCreate, MessageResponse, AttachmentResponse
from .analytics import DashboardStats, VelocityData, CycleTimeData

__all__ = [
    # Auth
    "LoginRequest", "RegisterRequest", "TokenResponse",
    "RefreshTokenRequest", "PasswordResetRequest", "PasswordChangeRequest",
    # User
    "UserCreate", "UserUpdate", "UserResponse", "UserBrief",
    # Team
    "TeamCreate", "TeamUpdate", "TeamResponse", "TeamWithMembers", "InviteQRResponse",
    # Board
    "BoardCreate", "BoardUpdate", "BoardResponse", "BoardWithColumns",
    "ColumnCreate", "ColumnUpdate", "ColumnResponse",
    # Task
    "TaskCreate", "TaskUpdate", "TaskResponse", "TaskBrief", "MoveTaskRequest",
    "LabelCreate", "LabelResponse",
    # Message
    "MessageCreate", "MessageResponse", "AttachmentResponse",
    # Analytics
    "DashboardStats", "VelocityData", "CycleTimeData"
]
