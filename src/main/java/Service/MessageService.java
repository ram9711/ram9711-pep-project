package Service;


import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import DAO.MessageDao;
import DAO.DaoException;
import Model.Account;
import Model.Message;
import io.javalin.http.NotFoundResponse;

public class MessageService {
    private MessageDao messageDao;
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageService.class);
    private static final String DB_ACCESS_ERROR_MSG = "Error accessing the database";

    // Default constructor initializing the MessageDao object
    public MessageService() {
        messageDao = new MessageDao();
    }

    // Constructor that allows an external MessageDao to be used, useful for testing
    // purposes.
    public MessageService(MessageDao messageDao) {
        this.messageDao = messageDao;
    }

    /**
     * Retrieve a Message by its ID using the MessageDao
     *
     * @param id The ID of the Message
     * @return Optional containing the found Message
     * @throws ServiceException If the Message is not found or there is a DAO
     *                          exception
     */
    public Optional<Message> getMessageById(int id) {
        LOGGER.info("Fetching message with ID: {} ", id);
        try {
            Optional<Message> message = messageDao.getById(id);
            if (!message.isPresent()) {
                throw new ServiceException("Message not found");
            }
            LOGGER.info("Fetched message: {}", message.orElse(null));
            return message;
        } catch (DaoException e) {
            throw new ServiceException(DB_ACCESS_ERROR_MSG, e);
        }
    }

    /**
     * Retrieve all messages using the MessageDao
     *
     * @return List of all Messages
     * @throws ServiceException If there is a DAO exception
     */
    public List<Message> getAllMessages() {
        LOGGER.info("Fetching all messages");
        try {
            List<Message> messages = messageDao.getAll();
            LOGGER.info("Fetched {} messages", messages.size());
            return messages;
        } catch (DaoException e) {
            throw new ServiceException(DB_ACCESS_ERROR_MSG, e);
        }
    }

    /**
     * Retrieve all messages posted by a specific account
     *
     * @param accountId The ID of the account
     * @return List of Messages posted by the account
     * @throws ServiceException If there is a DAO exception
     */
    public List<Message> getMessagesByAccountId(int accountId) {
        LOGGER.info("Fetching messages posted by ID account: {}", accountId);
        try {
            List<Message> messages = messageDao.getMessagesByAccountId(accountId);
            LOGGER.info("Fetched {} messages", messages.size());
            return messages;
        } catch (DaoException e) {
            throw new ServiceException(DB_ACCESS_ERROR_MSG, e);
        }
    }

    /**
     * Insert a new message into the database using the MessageDao.
     * Checks account permissions to ensure that only the message author can create
     * messages on their behalf.
     *
     * @param message The Message to create
     * @param account The Account creating the Message
     * @return The created Message
     * @throws ServiceException If the Account does not exist, the Message is not
     *                          valid, or there is a DAO exception
     */
    public Message createMessage(Message message, Optional<Account> account) {
        LOGGER.info("Creating message: {}", message);

        // Ensure that the account exists
        if (!account.isPresent()) {
            throw new ServiceException("Account must exist when posting a new message");
        }

        // Validate the message
        validateMessage(message);

        // Check account permission
        checkAccountPermission(account.get(), message.getPosted_by());
        try {
            // Insert the message into the database
            Message createdMessage = messageDao.insert(message);
            LOGGER.info("Created message: {}", createdMessage);
            return createdMessage;
        } catch (DaoException e) {
            throw new ServiceException(DB_ACCESS_ERROR_MSG, e);
        }
    }

    /**
     * Update an existing message in the database using the MessageDao.
     * Checks account permissions to ensure that only the message author can update
     * their own messages.
     *
     * @param message The Message to update
     * @return The updated Message
     * @throws ServiceException If the Message does not exist, is not valid, or
     *                          there is a DAO exception
     */
    public Message updateMessage(Message message) {
        LOGGER.info("Updating message: {}", message.getMessage_id());

        // Retrieve the existing message by its ID
        Optional<Message> retrievedMessage = this.getMessageById(message.getMessage_id());

        // Check if the message exists
        if (!retrievedMessage.isPresent()) {
            throw new ServiceException("Message not found");
        }

        // Update the message text with the new value
        retrievedMessage.get().setMessage_text(message.getMessage_text());

        // Validate the updated message
        validateMessage(retrievedMessage.get());

        try {
            // Update the message in the database
            messageDao.update(retrievedMessage.get());
            LOGGER.info("Updated message: {}", message);
            return retrievedMessage.get();
        } catch (DaoException e) {
            throw new ServiceException(DB_ACCESS_ERROR_MSG, e);
        }
    }

    /**
     * Delete an existing message from the database.
     * Check account permissions to ensure that only the message author can delete
     * their own messages.
     *
     * @param message The Message to delete
     * @throws ServiceException If the Message does not exist or there is a DAO
     *                          exception
     */
    public void deleteMessage(Message message) {
        LOGGER.info("Deleting message: {}", message);
        try {
            boolean hasDeletedMessage = messageDao.delete(message);
            if (hasDeletedMessage) {
                LOGGER.info("Deleted message {}", message);
            } else {
                throw new NotFoundResponse("Message to delete not found");
            }
        } catch (DaoException e) {
            throw new ServiceException(DB_ACCESS_ERROR_MSG, e);
        }
    }

    /**
     * Validate a message by checking if the message_text is null, empty, or
     * exceed the maximum allowed length.
     *
     * @param message The Message to validate
     * @throws ServiceException If the message is null, empty, or exceeds the
     *                          maximum length
     */
    private void validateMessage(Message message) {
        LOGGER.info("Validating message: {}", message);
        if (message.getMessage_text() == null || message.getMessage_text().trim().isEmpty()) {
            throw new ServiceException("Message text cannot be null or empty");
        }
        if (message.getMessage_text().length() > 254) {
            throw new ServiceException("Message text cannot exceed 254 characters");
        }
    }

    /**
     * Check if the account performing the action is the same as the one that posted
     * the message. This is used to maintain user data integrity and security.
     *
     * @param account  The Account that is performing the action
     * @param postedBy The ID of the account that posted the message
     * @throws ServiceException If the account is not authorized to modify the
     *                          message
     */
    private void checkAccountPermission(Account account, int postedBy) {
        LOGGER.info("Checking account permissions for messages");
        if (account.getAccount_id() != postedBy) {
            throw new ServiceException("Account not authorized to modify this message");
        }
    }
}
    

