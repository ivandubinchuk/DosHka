"""
Роутер вкладень
"""
import os
import uuid
import shutil
from typing import Annotated
from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, Form
from fastapi.responses import FileResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload

from ....core.database import get_db
from ....core.deps import CurrentUser
from ....models import Attachment, Task
from ....schemas import AttachmentResponse

router = APIRouter(prefix="/attachments", tags=["Вкладення"])

UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)


@router.get("/{task_id}", response_model=list[AttachmentResponse])
async def get_attachments(
    task_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Отримати вкладення задачі"""
    # Перевіряємо доступ до задачі
    task_result = await db.execute(select(Task).where(Task.id == task_id))
    task = task_result.scalar_one_or_none()

    if not task:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Задачу не знайдено"
        )

    # Виконавець може бачити вкладення тільки своїх задач
    if current_user.role == "executor" and task.assignee_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Немає доступу до цієї задачі"
        )

    result = await db.execute(
        select(Attachment)
        .options(selectinload(Attachment.uploader))
        .where(Attachment.task_id == task_id)
        .order_by(Attachment.created_at.desc())
    )
    return result.scalars().all()


@router.post("", response_model=AttachmentResponse)
async def upload_attachment(
    task_id: str = Form(...),
    file: UploadFile = File(...),
    current_user: CurrentUser = None,
    db: AsyncSession = Depends(get_db)
):
    """Завантажити вкладення"""
    # Перевіряємо чи задача існує
    task_result = await db.execute(select(Task).where(Task.id == task_id))
    task = task_result.scalar_one_or_none()
    if not task:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Задачу не знайдено"
        )

    # Виконавець може завантажувати вкладення тільки до своїх задач
    if current_user.role == "executor" and task.assignee_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Немає доступу до цієї задачі"
        )

    # Генеруємо унікальне ім'я файлу
    file_ext = os.path.splitext(file.filename)[1]
    unique_filename = f"{uuid.uuid4()}{file_ext}"
    file_path = os.path.join(UPLOAD_DIR, unique_filename)

    # Зберігаємо файл
    content = await file.read()
    with open(file_path, 'wb') as f:
        f.write(content)

    # Визначаємо тип файлу
    content_type = file.content_type or "application/octet-stream"
    if content_type.startswith("image/"):
        file_type = "image"
    elif content_type.startswith("audio/"):
        file_type = "audio"
    elif content_type.startswith("video/"):
        file_type = "video"
    elif content_type in ["application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument"]:
        file_type = "document"
    else:
        file_type = "other"

    # Створюємо запис в БД
    attachment = Attachment(
        task_id=task_id,
        uploader_id=current_user.id,
        file_name=file.filename,
        file_type=file_type,
        file_size=len(content),
        url=f"/api/v1/attachments/download/{unique_filename}"
    )
    db.add(attachment)
    await db.commit()

    # Перезавантажуємо з uploader
    result = await db.execute(
        select(Attachment)
        .options(selectinload(Attachment.uploader))
        .where(Attachment.id == attachment.id)
    )
    return result.scalar_one()


@router.get("/download/{filename}")
async def download_attachment(
    filename: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Завантажити файл"""
    file_path = os.path.join(UPLOAD_DIR, filename)

    if not os.path.exists(file_path):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Файл не знайдено"
        )

    return FileResponse(file_path, filename=filename)


@router.delete("/{attachment_id}")
async def delete_attachment(
    attachment_id: str,
    current_user: CurrentUser,
    db: Annotated[AsyncSession, Depends(get_db)]
):
    """Видалити вкладення"""
    result = await db.execute(
        select(Attachment).where(Attachment.id == attachment_id)
    )
    attachment = result.scalar_one_or_none()

    if not attachment:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Вкладення не знайдено"
        )

    # Видаляємо файл (витягуємо ім'я файлу з URL)
    if attachment.url:
        filename = attachment.url.split("/")[-1]
        file_path = os.path.join(UPLOAD_DIR, filename)
        if os.path.exists(file_path):
            os.remove(file_path)

    # Видаляємо запис
    await db.delete(attachment)
    await db.commit()

    return {"message": "Вкладення видалено"}
