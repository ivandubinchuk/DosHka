from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.models import User
from sqlalchemy import select
from uuid import uuid4

router = APIRouter(prefix="/executors", tags=["Executors"])


@router.post("/add")
async def add_executor(email: str, db: AsyncSession = Depends(get_db)):

    # перевірка дубліката
    result = await db.execute(
        select(User).where(User.email == email)
    )
    existing = result.scalar_one_or_none()

    if existing:
        return {"status": "error", "message": "Email already exists"}

    new_user = User(
        id=str(uuid4()),
        email=email,
        password_hash="123456",
        full_name="Новий користувач",
        role="executor",
        team_id=None,
        is_active=True
    )

    db.add(new_user)
    await db.commit()

    return {
        "status": "ok",
        "email": email
    }