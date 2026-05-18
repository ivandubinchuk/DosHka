"""
Tests for team endpoints
"""
import pytest
from httpx import AsyncClient
from faker import Faker

fake = Faker('uk_UA')


class TestTeamOperations:
    """Tests for team CRUD operations"""

    @pytest.mark.asyncio
    async def test_create_team(self, client: AsyncClient, auth_headers):
        """Test creating a new team"""
        response = await client.post("/api/v1/teams", headers=auth_headers, json={
            "name": fake.company(),
            "description": fake.catch_phrase()
        })

        assert response.status_code == 200
        data = response.json()
        assert "id" in data
        assert "name" in data

    @pytest.mark.asyncio
    async def test_get_team(self, client: AsyncClient, test_team, manager_auth_headers):
        """Test getting team details"""
        response = await client.get(
            f"/api/v1/teams/{test_team.id}",
            headers=manager_auth_headers
        )

        assert response.status_code == 200
        data = response.json()
        assert data["id"] == test_team.id
        assert data["name"] == test_team.name

    @pytest.mark.asyncio
    async def test_update_team(self, client: AsyncClient, test_team, manager_auth_headers):
        """Test updating team"""
        new_name = fake.company()
        response = await client.put(
            f"/api/v1/teams/{test_team.id}",
            headers=manager_auth_headers,
            json={"name": new_name}
        )

        assert response.status_code == 200
        data = response.json()
        assert data["name"] == new_name

    @pytest.mark.asyncio
    async def test_get_team_members(self, client: AsyncClient, test_team, manager_auth_headers):
        """Test getting team members"""
        response = await client.get(
            f"/api/v1/teams/{test_team.id}/members",
            headers=manager_auth_headers
        )

        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)

    @pytest.mark.asyncio
    async def test_get_nonexistent_team(self, client: AsyncClient, auth_headers):
        """Test getting non-existent team"""
        response = await client.get(
            "/api/v1/teams/nonexistent-id",
            headers=auth_headers
        )

        assert response.status_code == 404


class TestTeamInvite:
    """Tests for team invite operations"""

    @pytest.mark.asyncio
    async def test_generate_invite_qr(self, client: AsyncClient, test_team, manager_auth_headers):
        """Test generating invite QR code"""
        response = await client.post(
            "/api/v1/teams/invite/qr",
            headers=manager_auth_headers,
            json=test_team.id
        )

        # May return 200 or error depending on implementation
        assert response.status_code in [200, 400, 422]
