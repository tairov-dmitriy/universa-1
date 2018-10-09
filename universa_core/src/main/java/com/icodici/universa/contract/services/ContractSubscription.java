package com.icodici.universa.contract.services;

import com.icodici.universa.HashId;
import com.icodici.universa.contract.Contract;

import java.time.ZonedDateTime;

/**
 * Subscription to store one revision of the packed contract (transaction pack)
 * <p>
 * The subscribers ({@link NContract} instances) subscribe to contracts to store them for some amount of time. All
 * subscriptions share same copy of the stored contract. When the last susbscription to this revision is destroyed or
 * expired, the copy is dropped.
 * <p>
 * Note that subscriptions are private to {@link NContract} instances and visible only to it. When the NContract is
 * revoked, all its subscriptions must be destroyed.
 */

public interface ContractSubscription {

    ZonedDateTime expiresAt();

    /**
     * @return the unpacked stored contract. Note that this instance could be cached/shared among subscribers.
     */
    Contract getContract();

    /**
     * @return stored packed representation (transaction pack)
     */
    byte[] getPackedContract();

    /**
     * @return the origin of contracts chain of subscription.
     */
    HashId getOrigin();

    /**
     * The subscription event base interface. Real events are either {@link ApprovedEvent} or {@link RevokedEvent}
     * implementations.
     */
    interface Event {
        ContractSubscription getSubscription();
    }

    interface ApprovedEvent extends Event {
        /**
         * @return new revision just approved as the Contract
         */
        Contract getNewRevision();

        /**
         * @return Packed transaction of the new revision just approved
         */
        byte[] getPackedTransaction();


        MutableEnvironment getEnvironment();
    }

    interface RevokedEvent extends Event {
        ImmutableEnvironment getEnvironment();
    }

    interface CompletedEvent extends Event {
        MutableEnvironment getEnvironment();
    }

    interface FailedEvent extends Event {
        MutableEnvironment getEnvironment();
    }

    /**
     * Allow {@link NContract} to receive (or not) events with {@link Event}, with {@link
     * NContract#onContractSubscriptionEvent(Event)}
     *
     * @param doReceive true to receive events, false to stop
     */
    void receiveEvents(boolean doReceive);
}
