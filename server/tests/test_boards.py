"""
Tests for board endpoints
"""
import pytest
from httpx import AsyncClient
from faker import Faker

fake = Faker('uk_UA')


class TestBoardOperations:
    """Tests for board CRUD operations"""

    @pytest.mark.asyncio
    async def test_create_board(self, client: AsyncClient, test_team, manager_auth_headers):
        """Test creating a new board"""
        response = await client.post("/api/v1/boards", headers=manager_auth_headers, json={
            "name": "New Board",
            "description": "Test board",
            "team_id": test_team.id
        })

        assert response.status_code == 200
        data = response.json()
        assert data["name"] == "New Board"
        assert data["team_id"] == test_team.id

    @pytest.mark.asyncio
    async def test_get_board(self, client: AsyncClient, test_board, manager_auth_headers):
        """Test getting board details"""
        response = await client.get(
            f"/api/v1/boards/{test_board.id}",
            headers=manager_auth_headers
        )

        assert response.status_code == 200
        data = response.json()
        assert data["id"] == test_board.id

    @pytest.mark.asyncio
    async def test_get_boards_by_team(self, client: AsyncClient, test_team, test_board, manager_auth_headers):
        """Test getting all boards for a team"""
        response = await client.get(
            f"/api/v1/teams/{test_team.id}/boards",
            headers=manager_auth_headers
        )

        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) >= 1

    @pytest.mark.asyncio
    async def test_update_board(self, client: AsyncClient, test_board, test_team, manager_auth_headers):
        """Test updating board"""
        response = await client.put(
            f"/api/v1/boards/{test_board.id}",
            headers=manager_auth_headers,
            json={"name": "Updated Board", "team_id": test_team.id}
        )

        assert response.status_code == 200
        data = response.json()
        assert data["name"] == "Updated Board"

    @pytest.mark.asyncio
    async def test_delete_board(self, client: AsyncClient, test_board, manager_auth_headers):
        """Test deleting board"""
        response = await client.delete(
            f"/api/v1/boards/{test_board.id}",
            headers=manager_auth_headers
        )

        assert response.status_code == 200

    @pytest.mark.asyncio
    async def test_get_nonexistent_board(self, client: AsyncClient, auth_headers):
        """Test getting non-existent board"""
        response = await client.get(
            "/api/v1/boards/nonexistent-id",
            headers=auth_headers
        )

        assert response.status_code == 404


class TestColumnOperations:
    """Tests for column CRUD operations"""

    @pytest.mark.asyncio
    async def test_get_columns_by_board(self, client: AsyncClient, test_board, test_columns, auth_headers):
        """Test getting columns for a board"""
        response = await client.get(
            f"/api/v1/boards/{test_board.id}/columns",
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) == 4

    @pytest.mark.asyncio
    async def test_create_column(self, client: AsyncClient, test_board, manager_auth_headers):
        """Test creating a new column"""
        response = await client.post("/api/v1/columns", headers=manager_auth_headers, json={
            "name": "New Column",
            "board_id": test_board.id,
            "position": 5,
            "wip_limit": 10
        })

        assert response.status_code == 200
        data = response.json()
        assert data["name"] == "New Column"
        assert data["wip_limit"] == 10

    @pytest.mark.asyncio
    async def test_update_column(self, client: AsyncClient, test_columns, manager_auth_headers):
        """Test updating column"""
        column = test_columns[0]
        response = await client.put(
            f"/api/v1/columns/{column.id}",
            headers=manager_auth_headers,
            json={"name": "Updated Column", "board_id": column.board_id, "position": 0}
        )

        assert response.status_code == 200
        data = response.json()
        assert data["name"] == "Updated Column"

    @pytest.mark.asyncio
    async def test_delete_column(self, client: AsyncClient, test_columns, manager_auth_headers):
        """Test deleting column"""
        column = test_columns[0]
        response = await client.delete(
            f"/api/v1/columns/{column.id}",
            headers=manager_auth_headers
        )

        assert response.status_code == 200
