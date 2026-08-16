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
import com.patechltd.salexfypos.sync.SyncSerializer;
import com.patechltd.salexfypos.sync.SyncTracker;

import java.util.List;

@Dao
public abstract class AdminDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertUserRaw(User user);

    @Update
    abstract int updateUserRaw(User user);

    @Delete
    abstract int deleteUserRaw(User user);

    public long insertUser(User user) {
        long id = insertUserRaw(user);
        SyncTracker.track(SyncTracker.USER, user.uid, "INSERT", SyncSerializer.toJson(user));
        return id;
    }

    public int updateUser(User user) {
        int rows = updateUserRaw(user);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.USER, user.uid, "UPDATE", SyncSerializer.toJson(user));
        }
        return rows;
    }

    public int deleteUser(User user) {
        int rows = deleteUserRaw(user);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.USER, user.uid, "DELETE", "");
        }
        return rows;
    }

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    public abstract User findByUsername(String username);

    @Query("SELECT * FROM users WHERE id = :id")
    public abstract User getUser(String id);

    @Query("SELECT * FROM users ORDER BY fullName ASC")
    public abstract LiveData<List<User>> observeUsers();

    @Query("SELECT * FROM users ORDER BY fullName ASC")
    public abstract List<User> getUsers();

    @Query("SELECT COUNT(*) FROM users")
    public abstract int userCount();

    @Query("SELECT * FROM users WHERE username = :username AND id != :excludeId LIMIT 1")
    public abstract User findByUsernameExcluding(String username, String excludeId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertRoleRaw(Role role);

    @Update
    abstract int updateRoleRaw(Role role);

    @Delete
    abstract int deleteRoleRaw(Role role);

    public long insertRole(Role role) {
        long id = insertRoleRaw(role);
        SyncTracker.track(SyncTracker.ROLE, role.uid, "INSERT", SyncSerializer.toJson(role));
        return id;
    }

    public int updateRole(Role role) {
        int rows = updateRoleRaw(role);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.ROLE, role.uid, "UPDATE", SyncSerializer.toJson(role));
        }
        return rows;
    }

    public int deleteRole(Role role) {
        int rows = deleteRoleRaw(role);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.ROLE, role.uid, "DELETE", "");
        }
        return rows;
    }

    @Query("SELECT * FROM roles ORDER BY isDefault DESC, roleName ASC")
    public abstract LiveData<List<Role>> observeRoles();

    @Query("SELECT * FROM roles ORDER BY isDefault DESC, roleName ASC")
    public abstract List<Role> getRoles();

    @Query("SELECT * FROM roles WHERE id = :id")
    public abstract Role getRole(String id);

    @Query("SELECT * FROM roles WHERE roleName = :name LIMIT 1")
    public abstract Role findRoleByName(String name);

    @Query("SELECT COUNT(*) FROM roles")
    public abstract int roleCount();

    @Query("SELECT COUNT(*) FROM users WHERE roleId = :roleId")
    public abstract int userCountByRole(String roleId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long insertSetting(AppSetting setting);

    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    public abstract AppSetting getSetting(String key);

    @Query("SELECT * FROM app_settings")
    public abstract List<AppSetting> getSettings();
}
