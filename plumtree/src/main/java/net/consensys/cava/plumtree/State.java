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
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Local state to our peer, representing the make-up of the tree of peers.
 */
public final class State {

  private final PeerRepository peerRepository;
  private final MessageHashing messageHashingFunction;
  private final Map<Bytes, MessageHandler> messageHandlers = new ConcurrentHashMap<>();
  private final MessageSender messageSender;
  private final Queue<Runnable> lazyQueue = new ConcurrentLinkedQueue<>();
  private final Timer timer = new Timer("plumtree", true);
  private final long delay;

  final class MessageHandler {

    private final Bytes hash;
    private final AtomicBoolean receivedFullMessage = new AtomicBoolean(false);
    private final AtomicBoolean requestingGraftMessage = new AtomicBoolean(false);
    private List<TimerTask> tasks = new ArrayList<>();
    private List<Peer> lazyPeers = new ArrayList<>();

    MessageHandler(Bytes hash) {
      this.hash = hash;
    }

    void fullMessageReceived(Peer sender, Bytes message) {
      if (receivedFullMessage.compareAndSet(false, true)) {
        for (TimerTask task : tasks) {
          task.cancel();
        }
        for (Peer peer : peerRepository.eagerPushPeers()) {
          if (!sender.equals(peer)) {
            messageSender.sendMessage(MessageSender.Verb.GOSSIP, peer, message);
          }
        }
        lazyQueue.addAll(
            peerRepository
                .lazyPushPeers()
                .stream()
                .filter(p -> !lazyPeers.contains(p))
                .map(peer -> (Runnable) (() -> messageSender.sendMessage(MessageSender.Verb.IHAVE, peer, hash)))
                .collect(Collectors.toList()));
      } else {
        messageSender.sendMessage(MessageSender.Verb.PRUNE, sender, null);
        peerRepository.moveToLazy(sender);
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
          messageSender.sendMessage(MessageSender.Verb.GRAFT, lazyPeers.get(index), hash);
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

  public State(PeerRepository peerRepository, MessageHashing messageHashingFunction, MessageSender messageSender) {
    this(peerRepository, messageHashingFunction, messageSender, 5000, 5000);
  }

  public State(
      PeerRepository peerRepository,
      MessageHashing messageHashingFunction,
      MessageSender messageSender,
      long graftDelay,
      long lazyQueueInterval) {
    this.peerRepository = peerRepository;
    this.messageHashingFunction = messageHashingFunction;
    this.messageSender = messageSender;
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
   * Records a message was received in full from a peer
   *
   * @param peer the peer that sent the message
   * @param message the hash of the message
   */
  public void receiveFullMessage(Peer peer, Bytes message) {
    peerRepository.considerNewPeer(peer);
    MessageHandler handler = messageHandlers.computeIfAbsent(messageHashingFunction.hash(message), MessageHandler::new);
    handler.fullMessageReceived(peer, message);
  }

  /**
   * Records a message was partially received from a peer
   *
   * @param peer the peer that sent the message
   * @param messageHash the hash of the message
   */
  public void receiveHeaderMessage(Peer peer, Bytes messageHash) {
    MessageHandler handler = messageHandlers.computeIfAbsent(messageHash, MessageHandler::new);
    handler.partialMessageReceived(peer);
  }

  /**
   * Requests a peer be pruned away from the eager peers into the lazy peers
   * 
   * @param peer the peer to move to lazy peers
   */
  public void receivePruneMessage(Peer peer) {
    peerRepository.moveToLazy(peer);
  }

  /**
   * Requests a peer be grafted to the eager peers list
   * 
   * @param peer the peer to add to the eager peers
   * @param messageHash the hash of the message that triggers this grafting
   */
  public void receiveGraftMessage(Peer peer, Bytes messageHash) {
    peerRepository.moveToEager(peer);
    messageSender.sendMessage(MessageSender.Verb.GOSSIP, peer, messageHash);
  }

  void processQueue() {
    for (Runnable r : lazyQueue) {
      r.run();
    }
  }

  /**
   * Stops the gossip network state, cancelling all in progress tasks.
   */
  public void stop() {
    timer.cancel();
  }
}
