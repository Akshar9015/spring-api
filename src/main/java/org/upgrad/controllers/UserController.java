package org.upgrad.controllers;


import com.google.common.hash.Hashing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.upgrad.form.LoginUser;
import org.upgrad.form.RegisterNewUser;
import org.upgrad.models.User;
import org.upgrad.models.UserProfile;
import org.upgrad.services.NotificationService;
import org.upgrad.services.UserProfileService;
import org.upgrad.services.UserService;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/signup")
    public ResponseEntity signup(RegisterNewUser registerNewUser){
        User user = new User();
        user.setPassword(hashPassword(registerNewUser.getPassword()));
        user.setEmail(registerNewUser.getEmail());
        user.setUserName(registerNewUser.getUsername());
        UserProfile userProfile = new UserProfile();
        userProfile.setLastName(registerNewUser.getLastName());
        userProfile.setFirstName(registerNewUser.getFirstName());
        userProfile.setCountry(registerNewUser.getCountry());
        userProfile.setContactNumber(registerNewUser.getPhoneNumber());
        try {

            Date dob = new SimpleDateFormat("yyyy-MM-dd").parse(registerNewUser.getDateOfBirth());
            userProfile.setDob(dob);

        } catch (ParseException e) {e.printStackTrace();}
        userProfile.setAboutMe(registerNewUser.getAboutMe());

        String userPresent = String.valueOf(userService.findUserByUsername(registerNewUser.getUsername()));
        String userEmail = String.valueOf(userService.findUserByEmail(registerNewUser.getEmail()));

        if (!(userPresent.equalsIgnoreCase("null"))) {
            String message = "Try any other Username, this Username has already been taken.";
            return new ResponseEntity <> (message, HttpStatus.FORBIDDEN);
        } else if (!(userEmail.equalsIgnoreCase("null"))) {
            String message = "This user has already been registered, try with any other emailId.";
            return new ResponseEntity < > (message, HttpStatus.FORBIDDEN);
        } else {
            userService.registerUserDetails(user, userProfile);
            String message = registerNewUser.getUsername() + " successfully registered";
            return new ResponseEntity <> (message, HttpStatus.OK);
        }

    }

    @PostMapping("/login")
    public ResponseEntity<String> signin(LoginUser loginUser, HttpSession session)throws Exception
    {
        String message = null;
        String inputUserName = loginUser.getUsername();
        String inputPasswordHash = hashPassword(loginUser.getPassword());
        String userPasswordHash =  String.valueOf(userService.findUserPassword(loginUser.getUsername()));
        if (!(userPasswordHash.equalsIgnoreCase(inputPasswordHash))) {
            message = "Invalid Credentials";
            return new ResponseEntity <> (message, HttpStatus.UNAUTHORIZED);
        } else {
            String role = String.valueOf(userService.findUserRole(inputUserName));
            if (role.equalsIgnoreCase("admin")) {
                message = "You have logged in as admin!";
            } else if (role.equalsIgnoreCase("user")) {
                message = "You have logged in successfully!";
            }
            if(session.getAttribute("currUser")== null) {
                User user = userService.getUserByUsername(inputUserName);
                session.setAttribute("currUser", user);
            }
            return new ResponseEntity <> (message, HttpStatus.OK);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> signout(HttpSession session){

        if(session.getAttribute("currUser")== null)
            return new ResponseEntity<>("You are currently not logged in",HttpStatus.UNAUTHORIZED);
        else{
            session.removeAttribute("currUser");
            return new ResponseEntity<>("You have logged out successfully!",HttpStatus.OK);
        }

    }

    @GetMapping("/userprofile/{userId}")
    public ResponseEntity<?> userProfile(@PathVariable("userId") int userId, HttpSession session) {
        if (session.getAttribute("currUser") == null)
            return new ResponseEntity<>("Please Login first to access this endpoint", HttpStatus.UNAUTHORIZED);
        else {
            UserProfile userProfile = userProfileService.retrieveUserProfile(userId);
            if (userProfile!=null) {
                return new ResponseEntity<>(userProfile, HttpStatus.OK);
            }
            else {
                return new ResponseEntity<>("User Profile not found!", HttpStatus.NOT_FOUND);
            }
        }
    }


    /**
     * SHA-256 encryption
     * @param password the plain text String that we want to encrypt
     * @return the SAH-256 encrypted version of the password
     */
    private String hashPassword(String password) {
        return Hashing.sha256().hashString(password, StandardCharsets.UTF_8).toString();
    }

    @GetMapping("/notification/new")
    public ResponseEntity getNewNotifications(HttpSession session){
        if (session.getAttribute("currUser") == null)
            return new ResponseEntity<>("Please Login first to access this endpoint!", HttpStatus.UNAUTHORIZED);
        else {
            User currUser = (User)session.getAttribute("currUser");
            String currUserName = currUser.getUserName();
            int id = userService.findUserId(currUserName);
            return new ResponseEntity(notificationService.getNewNotifications(id),HttpStatus.OK);
        }
    }

    @GetMapping("/notification/all")
    public ResponseEntity getAllNotifications(HttpSession session){
        if (session.getAttribute("currUser") == null)
            return new ResponseEntity<>("Please Login first to access this endpoint!", HttpStatus.UNAUTHORIZED);
        else {
            User currUser = (User)session.getAttribute("currUser");
            String currUserName = currUser.getUserName();
            int id = userService.findUserId(currUserName);
            return new ResponseEntity<>(notificationService.getAllNotifications(id),HttpStatus.OK);
        }
    }

}