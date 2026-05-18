"""
Моделі повідомлень та вкладень
"""
import uuid
from datetime import datetime
from sqlalchemy import String, Boolean, DateTime, Integer, BigInteger, Text, ForeignKey, Index
from sqlalchemy.orm import Mapped, mapped_column, relationship
from ..core.database import Base


class Message(Base):
    """Таблиця повідомлень чату"""
    __tablename__ = "messages"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    task_id: Mapped[str] = mapped_column(String(36), ForeignKey("tasks.id", ondelete="CASCADE"), nullable=False)
    sender_id: Mapped[str | None] = mapped_column(String(36), ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    type: Mapped[str] = mapped_column(String(20), nullable=False, default="text")  # text, voice, system
    voice_url: Mapped[str | None] = mapped_column(String(500), nullable=True)
    voice_duration: Mapped[int | None] = mapped_column(Integer, nullable=True)  # секунди
    is_read: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    # Відносини
    task = relationship("Task", back_populates="messages")
    sender = relationship("User", back_populates="messages")

    __table_args__ = (
        Index("ix_messages_task_id", "task_id"),
        Index("ix_messages_created_at", "created_at"),
    )


class Attachment(Base):
    """Таблиця вкладень"""
    __tablename__ = "attachments"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    task_id: Mapped[str] = mapped_column(String(36), ForeignKey("tasks.id", ondelete="CASCADE"), nullable=False)
    uploader_id: Mapped[str | None] = mapped_column(String(36), ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    file_name: Mapped[str] = mapped_column(String(255), nullable=False)
    file_type: Mapped[str] = mapped_column(String(50), nullable=False)  # image, document, audio, other
    file_size: Mapped[int] = mapped_column(BigInteger, nullable=False)
    url: Mapped[str] = mapped_column(String(500), nullable=False)
    thumbnail_url: Mapped[str | None] = mapped_column(String(500), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    # Відносини
    task = relationship("Task", back_populates="attachments")
    uploader = relationship("User")

    __table_args__ = (
        Index("ix_attachments_task_id", "task_id"),
    )
