package au.com.noojee.acceloUI.authentication;

import au.com.noojee.acceloapi.dao.StaffDao;
import au.com.noojee.acceloapi.entities.Staff;

/**
 * Default mock implementation of {@link AccessControl}. This implementation
 * accepts any string as a password, and considers the user "admin" as the only
 * administrator.
 */
@SuppressWarnings("serial")
public class BasicAccessControl implements AccessControl {

    @Override
    public boolean signIn(String email, String password) {
        if (email == null || email.isEmpty())
            return false;

        Staff staff = new StaffDao().getByEmail(email);
        
        CurrentUser.set(staff);
        return true;
    }

    @Override
    public boolean isUserSignedIn() {
        return CurrentUser.isUserSignedIn();
    }

    @Override
    public boolean isUserInRole(String role) {
        if ("admin".equals(role)) {
            // Only the "admin" user is in the "admin" role
            return getPrincipalName().equals("bsutton@noojee.com.au");
        }

        // All users are in all non-admin roles
        return true;
    }

    @Override
    public String getPrincipalName() {
        return CurrentUser.get().staff.getEmail();
    }

}
