/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.plumtree;

import org.apache.tuweni.bytes.Bytes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Local state to our peer, representing the make-up of the tree of peers.
 */
public final class State {

  private final PeerRepository peerRepository;
  private final MessageHashing messageHashingFunction;
  private final int maxMessagesHandlers = 1000000;
  private final Map<Bytes, MessageHandler> messageHandlers =
      Collections.synchronizedMap(new LinkedHashMap<Bytes, MessageHandler>() {

        @Override
        protected boolean removeEldestEntry(final Map.Entry<Bytes, MessageHandler> eldest) {
          return super.size() > maxMessagesHandlers;
        }
      });

  private final MessageSender messageSender;
  private final MessageListener messageListener;
  private final MessageValidator messageValidator;
  private final PeerPruning peerPruningFunction;
  final Queue<Runnable> lazyQueue = new ConcurrentLinkedQueue<>();
  private final Timer timer = new Timer("plumtree", true);
  private final long delay;

  public void sendMessage(Peer peer, String attributes, Bytes message) {
    Bytes messageHash = messageHashingFunction.hash(message);
    messageSender.sendMessage(MessageSender.Verb.SEND, attributes, peer, messageHash, message);
  }

  final class MessageHandler {

    private final Bytes hash;

    private final AtomicBoolean receivedFullMessage = new AtomicBoolean(false);
    private final AtomicBoolean requestingGraftMessage = new AtomicBoolean(false);
    private List<TimerTask> tasks = new ArrayList<>();
    private List<Peer> lazyPeers = new ArrayList<>();

    MessageHandler(Bytes hash) {
      this.hash = hash;
    }

    /**
     * Acts on receiving the full message.
     * 
     * @param sender the sender - may be null if we are submitting this message to the network
     * @param message the payload to send to the network
     */
    void fullMessageReceived(@Nullable Peer sender, String attributes, Bytes message) {
      if (receivedFullMessage.compareAndSet(false, true)) {
        for (TimerTask task : tasks) {
          task.cancel();
        }

        if (sender == null || messageValidator.validate(message, sender)) {
          for (Peer peer : peerRepository.eagerPushPeers()) {
            if (sender == null || !sender.equals(peer)) {
              messageSender.sendMessage(MessageSender.Verb.GOSSIP, attributes, peer, hash, message);
            }
          }
          lazyQueue
              .addAll(
                  peerRepository
                      .lazyPushPeers()
                      .stream()
                      .filter(p -> !lazyPeers.contains(p))
                      .map(
                          peer -> (Runnable) (() -> messageSender
                              .sendMessage(MessageSender.Verb.IHAVE, null, peer, hash, null)))
                      .collect(Collectors.toList()));
          if (sender != null) {
            messageListener.listen(message, attributes, sender);
          }
        }
      } else {
        if (sender != null) {
          if (peerPruningFunction.prunePeer(sender)) {
            messageSender.sendMessage(MessageSender.Verb.PRUNE, null, sender, hash, null);
          }
        }
      }
    }

    private void scheduleGraftMessage(final int index) {
      TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
          int newPeerIndex = index;
          if (newPeerIndex == lazyPeers.size()) {
            newPeerIndex = 0;
          }
          messageSender.sendMessage(MessageSender.Verb.GRAFT, null, lazyPeers.get(index), hash, null);
          scheduleGraftMessage(newPeerIndex++);
        }
      };
      tasks.add(timerTask);
      timer.schedule(timerTask, delay);
    }

    void partialMessageReceived(Peer peer) {
      if (!receivedFullMessage.get()) {
        lazyPeers.add(peer);
        if (requestingGraftMessage.compareAndSet(false, true)) {
          scheduleGraftMessage(0);
        }
      }
    }
  }

  /**
   * Constructor using default time constants.
   * 
   * @param peerRepository the peer repository to use to store and access peer information.
   * @param messageHashingFunction the function to use to hash messages into hashes to compare them.
   * @param messageSender a function abstracting sending messages to other peers.
   * @param messageListener a function consuming messages when they are gossiped.
   * @param messageValidator a function validating messages before they are gossiped to other peers.
   * @param peerPruningFunction a function deciding whether to prune peers.
   */
  public State(
      PeerRepository peerRepository,
      MessageHashing messageHashingFunction,
      MessageSender messageSender,
      MessageListener messageListener,
      MessageValidator messageValidator,
      PeerPruning peerPruningFunction) {
    this(
        peerRepository,
        messageHashingFunction,
        messageSender,
        messageListener,
        messageValidator,
        peerPruningFunction,
        5000,
        5000);
  }

  /**
   * Default constructor.
   * 
   * @param peerRepository the peer repository to use to store and access peer information.
   * @param messageHashingFunction the function to use to hash messages into hashes to compare them.
   * @param messageSender a function abstracting sending messages to other peers.
   * @param messageListener a function consuming messages when they are gossiped.
   * @param messageValidator a function validating messages before they are gossiped to other peers.
   * @param peerPruningFunction a function deciding whether to prune peers.
   * @param graftDelay delay in milliseconds to apply before this peer grafts an other peer when it finds that peer has
   *        data it misses.
   * @param lazyQueueInterval the interval in milliseconds between sending messages to lazy peers.
   */
  public State(
      PeerRepository peerRepository,
      MessageHashing messageHashingFunction,
      MessageSender messageSender,
      MessageListener messageListener,
      MessageValidator messageValidator,
      PeerPruning peerPruningFunction,
      long graftDelay,
      long lazyQueueInterval) {
    this.peerRepository = peerRepository;
    this.messageHashingFunction = messageHashingFunction;
    this.messageSender = messageSender;
    this.messageListener = messageListener;
    this.messageValidator = messageValidator;
    this.peerPruningFunction = peerPruningFunction;
    this.delay = graftDelay;
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        processQueue();
      }
    }, lazyQueueInterval, lazyQueueInterval);
  }

  /**
   * Adds a new peer to this state.
   *
   * @param peer the new peer
   */
  public void addPeer(Peer peer) {
    peerRepository.addEager(peer);
  }

  /**
   * Removes a peer from the collection of peers we are connected to.
   *
   * @param peer the peer to remove
   */
  public void removePeer(Peer peer) {
    peerRepository.removePeer(peer);

  }

  /**
   * Records a message was received in full from a peer.
   *
   * @param peer the peer that sent the message
   * @param attributes of the message
   * @param message the message
   * @param messageHash the hash of the message
   */
  public void receiveGossipMessage(Peer peer, String attributes, Bytes message, Bytes messageHash) {
    Bytes checkHash = messageHashingFunction.hash(message);
    if (!checkHash.equals(messageHash)) {
      return;
    }
    peerRepository.considerNewPeer(peer);
    MessageHandler handler = messageHandlers.computeIfAbsent(messageHash, MessageHandler::new);
    handler.fullMessageReceived(peer, attributes, message);
  }

  /**
   * Records a message was partially received from a peer.
   *
   * @param peer the peer that sent the message
   * @param messageHash the hash of the message
   */
  public void receiveIHaveMessage(Peer peer, Bytes messageHash) {
    MessageHandler handler = messageHandlers.computeIfAbsent(messageHash, MessageHandler::new);
    handler.partialMessageReceived(peer);
  }

  /**
   * Requests a peer be pruned away from the eager peers into the lazy peers.
   *
   * @param peer the peer to move to lazy peers
   */
  public void receivePruneMessage(Peer peer) {
    peerRepository.moveToLazy(peer);
  }

  /**
   * Requests a peer be grafted to the eager peers list.
   *
   * @param peer the peer to add to the eager peers
   * @param messageHash the hash of the message that triggers this grafting
   */
  public void receiveGraftMessage(Peer peer, Bytes messageHash) {
    peerRepository.moveToEager(peer);
    messageSender.sendMessage(MessageSender.Verb.GOSSIP, null, peer, messageHash, null);
  }

  /**
   * Sends a gossip message to all peers, according to their status.
   * 
   * @param message the message to propagate
   * @param attributes of the message
   * @return The associated hash of the message
   */
  public Bytes sendGossipMessage(String attributes, Bytes message) {
    Bytes messageHash = messageHashingFunction.hash(message);
    MessageHandler handler = messageHandlers.computeIfAbsent(messageHash, MessageHandler::new);
    handler.fullMessageReceived(null, attributes, message);
    return messageHash;
  }

  void processQueue() {
    List<Runnable> executed = new ArrayList<>();
    for (Runnable r : lazyQueue) {
      r.run();
      executed.add(r);
    }
    lazyQueue.removeAll(executed);
  }

  /**
   * Stops the gossip network state, cancelling all in progress tasks.
   */
  public void stop() {
    timer.cancel();
  }
}
