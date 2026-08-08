package com.patechltd.salexfypos.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.patechltd.salexfypos.db.entity.AppSetting;
import com.patechltd.salexfypos.db.entity.Role;
import com.patechltd.salexfypos.db.entity.User;

import java.util.List;

@Dao
public interface AdminDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUser(User user);

    @Update
    int updateUser(User user);

    @Delete
    int deleteUser(User user);

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User findByUsername(String username);

    @Query("SELECT * FROM users WHERE id = :id")
    User getUser(String id);

    @Query("SELECT * FROM users ORDER BY fullName ASC")
    LiveData<List<User>> observeUsers();

    @Query("SELECT * FROM users ORDER BY fullName ASC")
    List<User> getUsers();

    @Query("SELECT COUNT(*) FROM users")
    int userCount();

    @Query("SELECT * FROM users WHERE username = :username AND id != :excludeId LIMIT 1")
    User findByUsernameExcluding(String username, String excludeId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertRole(Role role);

    @Update
    int updateRole(Role role);

    @Delete
    int deleteRole(Role role);

    @Query("SELECT * FROM roles ORDER BY isDefault DESC, roleName ASC")
    LiveData<List<Role>> observeRoles();

    @Query("SELECT * FROM roles ORDER BY isDefault DESC, roleName ASC")
    List<Role> getRoles();

    @Query("SELECT * FROM roles WHERE id = :id")
    Role getRole(String id);

    @Query("SELECT * FROM roles WHERE roleName = :name LIMIT 1")
    Role findRoleByName(String name);

    @Query("SELECT COUNT(*) FROM roles")
    int roleCount();

    @Query("SELECT COUNT(*) FROM users WHERE roleId = :roleId")
    int userCountByRole(String roleId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSetting(AppSetting setting);

    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    AppSetting getSetting(String key);

    @Query("SELECT * FROM app_settings")
    List<AppSetting> getSettings();
}
