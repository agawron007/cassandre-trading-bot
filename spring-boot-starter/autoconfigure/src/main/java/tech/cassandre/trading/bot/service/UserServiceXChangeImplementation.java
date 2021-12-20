package tech.cassandre.trading.bot.service;

import tech.cassandre.trading.bot.dto.user.AccountDTO;
import tech.cassandre.trading.bot.dto.user.UserDTO;
import tech.cassandre.trading.bot.util.base.service.BaseService;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User service - XChange implementation.
 */
public class UserServiceXChangeImplementation extends BaseService implements UserService {

    /** XChange service. */
    private final org.knowm.xchange.service.account.AccountService xChangeAccountService;

    /** Cached reply from Exchange. */
    private final Map<String, AccountDTO> cachedReply = new ConcurrentHashMap<>();

    /**
     * Constructor.
     *
     * @param rate                     rate in ms
     * @param newXChangeAccountService xchange account service
     */
    public UserServiceXChangeImplementation(final long rate, final org.knowm.xchange.service.account.AccountService newXChangeAccountService) {
        super(rate);
        this.xChangeAccountService = newXChangeAccountService;
    }

    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public Optional<UserDTO> getUser() {
        try {
            // Consume a token from the token bucket.
            // If a token is not available this method will block until the refill adds one to the bucket.
            bucket.tryConsume(1);

            logger.debug("Retrieving account information");
            final UserDTO user = accountMapper.mapToUserDTO(xChangeAccountService.getAccountInfo());
            logger.debug("Account information retrieved " + user);
            // Update the cached reply.
            if (user != null && user.getAccounts() != null) {
                user.getAccounts()
                        .values()
                        .stream()
                        .filter(accountDTO -> accountDTO.getAccountId() != null)
                        .forEach(accountDTO -> cachedReply.put(accountDTO.getAccountId(), accountDTO));
            }
            return Optional.ofNullable(user);
        } catch (IOException e) {
            logger.error("Error retrieving account information: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @SuppressWarnings("checkstyle:DesignForExtension")
    public Map<String, AccountDTO> getAccounts() {
        final Optional<UserDTO> user = getUser();
        if (user.isPresent()) {
            return user.get().getAccounts();
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public final Map<String, AccountDTO> getAccountsFromCache() {
        if (cachedReply.isEmpty()) {
            return getAccounts();
        } else {
            return cachedReply;
        }
    }

}
