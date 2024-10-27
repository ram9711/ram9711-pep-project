package Controller;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import Model.Account;
import Model.Message;
import Service.AccountService;
import Service.MessageService;
import Service.ServiceException;

import io.javalin.Javalin;
import io.javalin.http.Context;

/**
 * TODO: You will need to write your own endpoints and handlers for your controller. The endpoints you will need can be
 * found in readme.md as well as the test cases. You should
 * refer to prior mini-project labs and lecture materials for guidance on how a controller may be built.
 */
public class SocialMediaController {
    private final AccountService accountService;
    private final MessageService messageService;

    public SocialMediaController() {
        // Initialize the accountService and messageService instances
        this.accountService = new AccountService();
        this.messageService = new MessageService();
    }
    /**
     * In order for the test cases to work, you will need to write the endpoints in the startAPI() method, as the test
     * suite must receive a Javalin object from this method.
     * @return a Javalin app object which defines the behavior of the Javalin controller.
     */
    public Javalin startAPI() {
        Javalin app = Javalin.create();
        app.post("/register", this::registerAccount);
        app.post("/login", this::loginAccount);
        app.post("/messages", this::createMessage);
        app.get("/messages", this::getAllMessages);
        app.get("/messages/{message_id}", this::getMessageById);
        app.delete("/messages/{message_id}", this::deleteMessageById);
        app.patch("/messages/{message_id}", this::updateMessageById);
        app.get("/accounts/{account_id}/messages",
                this::getMessagesByAccountId);
       
        

        return app;
    }

    /**
     * This is an example handler for an example endpoint.
     * @param context The Javalin Context object manages information about both the HTTP request and response.
     */
    private void registerAccount(Context ctx) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Account account = mapper.readValue(ctx.body(), Account.class);
        try {
            Account registeredAccount = accountService.createAccount(account);

            // Send the registered account as a JSON response
            ctx.json(mapper.writeValueAsString(registeredAccount));
        } catch (ServiceException e) {
            // Set the response status to 400 (Bad Request) in case of exception
            ctx.status(400);
        }
    }

    /**
     * This method handles the login process for users.
     * It expects a POST request to "/login" with the account credentials in the
     * request body.
     *
     * @param ctx the Javalin context object representing the current HTTP request
     *            and response
     * @throws JsonProcessingException if an error occurs during JSON parsing or
     *                                 serialization
     */
    private void loginAccount(Context ctx) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper(); // it calls a default no-arg constructor from Model.Account - REQUIRED
                                                  // for Jackson ObjectMapper
        Account account = mapper.readValue(ctx.body(), Account.class);

        try {
            Optional<Account> loggedInAccount = accountService
                    .validateLogin(account);
            if (loggedInAccount.isPresent()) {
                // Send the logged-in account as a JSON response
                ctx.json(mapper.writeValueAsString(loggedInAccount));
                ctx.sessionAttribute("logged_in_account",
                        loggedInAccount.get());
                ctx.json(loggedInAccount.get());
            } else {
                // Set the response status to 401 (Unauthorized) if the account is not found
                ctx.status(401);
            }
        } catch (ServiceException e) {
            // Set the response status to 401 (Unauthorized) in case of exception
            ctx.status(401);
        }
    }

    /**
     * This method handles the creation of new messages.
     * It expects a POST request to "/messages" with the message details in the
     * request body.
     *
     * @param ctx the Javalin context object representing the current HTTP request
     *            and response
     * @throws JsonProcessingException if an error occurs during JSON parsing or
     *                                 serialization
     */
    private void createMessage(Context ctx) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Message mappedMessage = mapper.readValue(ctx.body(), Message.class);
        try {
            Optional<Account> account = accountService
                    .getAccountById(mappedMessage.getPosted_by());
            Message message = messageService.createMessage(mappedMessage,
                    account);
            ctx.json(message);
        } catch (ServiceException e) {
            // Set the response status to 400 (Bad Request) in case of exception
            ctx.status(400);
        }
    }

    /**
     * This method retrieves all messages.
     * It expects a GET request to "/messages".
     *
     * @param ctx the Javalin context object representing the current HTTP request
     *            and response
     */
    private void getAllMessages(Context ctx) {

        List<Message> messages = messageService.getAllMessages();
        ctx.json(messages);
    }

    /**
     * This method handles the retrieval of a specific message by its ID.
     * It expects a GET request to "/messages/{message_id}".
     *
     * @param ctx the Javalin context object representing the current HTTP request
     *            and response
     */

    private void getMessageById(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("message_id"));
            Optional<Message> message = messageService.getMessageById(id);
            if (message.isPresent()) {
                ctx.json(message.get());
            } else {
                // If the message is not found, set the response status to 200 (OK)
                ctx.status(200); // As per test expectations, return a 200 status even if the message is not
                                 // found.
                ctx.result(""); // Response body is empty as the message was not found.
            }
            // Catch block for NumberFormatException is required to handle cases where the
            // 'message_id' path parameter cannot be parsed to an integer. Without this, an
            // invalid 'message_id' could lead to unhandled exceptions and potential
            // application crashes.
        } catch (NumberFormatException e) {
            ctx.status(400); // Respond with a 'Bad Request' status for invalid 'message_id'.
        } catch (ServiceException e) {
            ctx.status(200); // Respond with a '200' status even in case of a service error.
            ctx.result(""); // Response body is empty as there was a service error.
        }
    }

    /**
     * This method handles the deletion of a specific message by its ID.
     * It expects a DELETE request to "/messages/{message_id}".
     *
     * @param ctx the Javalin context object representing the current HTTP request
     *            and response
     */
    private void deleteMessageById(Context ctx) {
        try {
            // Retrieve the message ID from the path parameter
            int id = Integer.parseInt(ctx.pathParam("message_id"));

            // Attempt to retrieve the message by its ID
            Optional<Message> message = messageService.getMessageById(id);
            if (message.isPresent()) {
                // The message exists, so delete it
                messageService.deleteMessage(message.get());
                ctx.status(200);
                // Include the deleted message in the response body
                ctx.json(message.get());
            } else {
                // The message does not exist
                // Set the response status to 200 (OK) to indicate successful deletion
                ctx.status(200);
            }
        } catch (ServiceException e) {
            // An exception occurred during the deletion process
            // Set the response status to 200 (OK) to handle the exception gracefully
            ctx.status(200);
        }
    }

    /**
     * This method handles the update of a specific message by its ID.
     * It expects a PATCH request to "/messages/{message_id}" with the new content
     * of the message in the request body.
     *
     * @param ctx the Javalin context object representing the current HTTP request
     *            and response
     * @throws JsonProcessingException if an error occurs during JSON parsing or
     *                                 serialization
     */
    private void updateMessageById(Context ctx) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Message mappedMessage = mapper.readValue(ctx.body(), Message.class);
        try {
            int id = Integer.parseInt(ctx.pathParam("message_id"));
            mappedMessage.setMessage_id(id);

            // Update the message with the new content
            Message messageUpdated = messageService
                    .updateMessage(mappedMessage);

            // Set the response status to 200 (OK) and include the updated message in the
            // response body
            ctx.json(messageUpdated);

        } catch (ServiceException e) {
            // An exception occurred during the update process
            // Set the response status to 400 (Bad Request) to indicate a failure in the
            // request
            ctx.status(400);
        }
    }

    /**
     * This method retrieves all messages associated with a specific account ID.
     * It expects a GET request to "/accounts/{account_id}/messages".
     *
     * @param ctx the Javalin context object representing the current HTTP request
     *            and response
     */
    private void getMessagesByAccountId(Context ctx) {
        try {
            int accountId = Integer.parseInt(ctx.pathParam("account_id"));

            // Call the messageService to retrieve messages by account ID
            List<Message> messages = messageService
                    .getMessagesByAccountId(accountId);
            if (!messages.isEmpty()) {
                // If messages are found, send them as a JSON response
                ctx.json(messages);
            } else {
                // If no messages are found, send an empty JSON response
                ctx.json(messages);
                ctx.status(200);
            }
        } catch (ServiceException e) {
            // Handle ServiceException and set the status code to 400 (Bad Request)
            ctx.status(400);
        }
    }
}













// the code is executed successfully
