"""
Pytest fixtures for DoshKa server tests
"""
import asyncio
import pytest
from typing import AsyncGenerator, Generator
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from faker import Faker

from app.main import app
from app.core.database import Base, get_db
from app.core.security import create_access_token, hash_password
from app.models import User, Team, Board, Column, Task, Label

# Test database URL
TEST_DATABASE_URL = "sqlite+aiosqlite:///./test_doshka.db"

fake = Faker('uk_UA')


@pytest.fixture(scope="session")
def event_loop() -> Generator:
    """Create event loop for async tests"""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest.fixture(scope="function")
async def test_engine():
    """Create test database engine"""
    engine = create_async_engine(
        TEST_DATABASE_URL,
        echo=False,
        future=True
    )

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    yield engine

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)

    await engine.dispose()


@pytest.fixture(scope="function")
async def db_session(test_engine) -> AsyncGenerator[AsyncSession, None]:
    """Create database session for tests"""
    async_session = async_sessionmaker(
        test_engine,
        class_=AsyncSession,
        expire_on_commit=False
    )

    async with async_session() as session:
        yield session
        await session.rollback()


@pytest.fixture(scope="function")
async def client(db_session: AsyncSession) -> AsyncGenerator[AsyncClient, None]:
    """Create test HTTP client"""

    async def override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = override_get_db

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac

    app.dependency_overrides.clear()


@pytest.fixture
async def test_user(db_session: AsyncSession) -> User:
    """Create test user"""
    user = User(
        email=fake.email(),
        password_hash=hash_password("TestPassword123!"),
        full_name=fake.name(),
        role="member"
    )
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest.fixture
async def test_manager(db_session: AsyncSession) -> User:
    """Create test manager user"""
    user = User(
        email=fake.email(),
        password_hash=hash_password("ManagerPass123!"),
        full_name=fake.name(),
        role="manager"
    )
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest.fixture
async def test_team(db_session: AsyncSession, test_manager: User) -> Team:
    """Create test team"""
    team = Team(
        name=fake.company(),
        description=fake.catch_phrase(),
        manager_id=test_manager.id
    )
    db_session.add(team)
    await db_session.commit()
    await db_session.refresh(team)

    # Add manager to team
    test_manager.team_id = team.id
    await db_session.commit()

    return team


@pytest.fixture
async def test_board(db_session: AsyncSession, test_team: Team) -> Board:
    """Create test board"""
    board = Board(
        name="Test Board",
        description="Test board description",
        team_id=test_team.id,
        is_default=True
    )
    db_session.add(board)
    await db_session.commit()
    await db_session.refresh(board)
    return board


@pytest.fixture
async def test_columns(db_session: AsyncSession, test_board: Board) -> list[Column]:
    """Create test columns"""
    columns_data = [
        {"name": "Очікує", "position": 0, "wip_limit": None},
        {"name": "В роботі", "position": 1, "wip_limit": 5},
        {"name": "На перевірці", "position": 2, "wip_limit": 3},
        {"name": "Завершено", "position": 3, "wip_limit": None},
    ]

    columns = []
    for data in columns_data:
        column = Column(
            name=data["name"],
            board_id=test_board.id,
            position=data["position"],
            wip_limit=data["wip_limit"]
        )
        db_session.add(column)
        columns.append(column)

    await db_session.commit()

    for col in columns:
        await db_session.refresh(col)

    return columns


@pytest.fixture
async def test_task(db_session: AsyncSession, test_columns: list[Column], test_user: User) -> Task:
    """Create test task"""
    task = Task(
        title="Test Task",
        description="Test task description",
        column_id=test_columns[0].id,
        position=0,
        priority="medium",
        assignee_id=test_user.id,
        creator_id=test_user.id,
        tags='["test", "fixture"]'
    )
    db_session.add(task)
    await db_session.commit()
    await db_session.refresh(task)
    return task


@pytest.fixture
async def test_label(db_session: AsyncSession, test_team: Team) -> Label:
    """Create test label"""
    label = Label(
        name="Bug",
        color="#FF1744",
        team_id=test_team.id
    )
    db_session.add(label)
    await db_session.commit()
    await db_session.refresh(label)
    return label


@pytest.fixture
def auth_headers(test_user: User) -> dict:
    """Create auth headers for test user"""
    token = create_access_token(data={"sub": test_user.id})
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture
def manager_auth_headers(test_manager: User) -> dict:
    """Create auth headers for test manager"""
    token = create_access_token(data={"sub": test_manager.id})
    return {"Authorization": f"Bearer {token}"}
