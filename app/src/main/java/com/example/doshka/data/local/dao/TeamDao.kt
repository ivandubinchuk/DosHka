package com.example.doshka.data.local.dao

import androidx.room.*
import com.example.doshka.data.local.entity.TeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {

    @Query("SELECT * FROM teams WHERE id = :teamId")
    suspend fun getTeamById(teamId: String): TeamEntity?

    @Query("SELECT * FROM teams WHERE id = :teamId")
    fun observeTeamById(teamId: String): Flow<TeamEntity?>

    @Query("SELECT * FROM teams WHERE managerId = :managerId")
    fun observeTeamsByManager(managerId: String): Flow<List<TeamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeams(teams: List<TeamEntity>)

    @Update
    suspend fun updateTeam(team: TeamEntity)

    @Query("DELETE FROM teams WHERE id = :teamId")
    suspend fun deleteTeam(teamId: String)

    @Query("DELETE FROM teams")
    suspend fun deleteAllTeams()
}
