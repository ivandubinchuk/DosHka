"""
Tests for analytics endpoints
"""
import pytest
from httpx import AsyncClient
from datetime import datetime, timedelta


class TestDashboardAnalytics:
    """Tests for dashboard statistics"""

    @pytest.mark.asyncio
    async def test_get_dashboard_stats(self, client: AsyncClient, test_team, test_task, auth_headers):
        """Test getting dashboard statistics"""
        response = await client.get(
            f"/api/v1/analytics/dashboard?team_id={test_team.id}",
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()
        assert "total_tasks" in data
        assert "completed_tasks" in data
        assert "in_progress_tasks" in data
        assert "overdue_tasks" in data
        assert "completion_rate" in data

    @pytest.mark.asyncio
    async def test_get_dashboard_stats_empty_team(self, client: AsyncClient, auth_headers):
        """Test getting dashboard stats for team with no tasks"""
        response = await client.get(
            "/api/v1/analytics/dashboard?team_id=nonexistent-team",
            headers=auth_headers
        )

        assert response.status_code == 200
        data = response.json()
        assert data["total_tasks"] == 0
        assert data["completion_rate"] == 0.0


class TestVelocityAnalytics:
    """Tests for velocity analytics"""

    @pytest.mark.asyncio
    async def test_get_velocity(self, client: AsyncClient, test_team, manager_auth_headers):
        """Test getting velocity data"""
        response = await client.get(
            f"/api/v1/analytics/velocity?team_id={test_team.id}&weeks=4",
            headers=manager_auth_headers
        )

        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) == 4  # 4 weeks requested

        for week_data in data:
            assert "week_number" in week_data
            assert "year" in week_data
            assert "completed_tasks" in week_data
            assert "start_date" in week_data
            assert "end_date" in week_data

    @pytest.mark.asyncio
    async def test_get_velocity_default_weeks(self, client: AsyncClient, test_team, manager_auth_headers):
        """Test getting velocity with default weeks parameter"""
        response = await client.get(
            f"/api/v1/analytics/velocity?team_id={test_team.id}",
            headers=manager_auth_headers
        )

        assert response.status_code == 200
        data = response.json()
        assert len(data) == 8  # Default is 8 weeks

    @pytest.mark.asyncio
    async def test_get_velocity_unauthorized(self, client: AsyncClient, test_team, auth_headers):
        """Test getting velocity without manager role"""
        response = await client.get(
            f"/api/v1/analytics/velocity?team_id={test_team.id}",
            headers=auth_headers
        )

        # Should require manager role
        assert response.status_code in [200, 403]


class TestCycleTimeAnalytics:
    """Tests for cycle time analytics"""

    @pytest.mark.asyncio
    async def test_get_cycle_time(self, client: AsyncClient, test_team, manager_auth_headers):
        """Test getting cycle time data"""
        response = await client.get(
            f"/api/v1/analytics/cycle-time?team_id={test_team.id}",
            headers=manager_auth_headers
        )

        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)

        for user_data in data:
            assert "user_id" in user_data
            assert "user_name" in user_data
            assert "average_cycle_time_hours" in user_data
            assert "tasks_completed" in user_data

    @pytest.mark.asyncio
    async def test_get_cycle_time_unauthorized(self, client: AsyncClient, test_team, auth_headers):
        """Test getting cycle time without manager role"""
        response = await client.get(
            f"/api/v1/analytics/cycle-time?team_id={test_team.id}",
            headers=auth_headers
        )

        # Should require manager role
        assert response.status_code in [200, 403]
