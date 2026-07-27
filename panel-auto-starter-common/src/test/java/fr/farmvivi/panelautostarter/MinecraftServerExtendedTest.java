package fr.farmvivi.panelautostarter;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class MinecraftServerExtendedTest {

    @Test
    public void testMinecraftServerStatusEnum() {
        MinecraftServerStatus offline = MinecraftServerStatus.OFFLINE;
        MinecraftServerStatus starting = MinecraftServerStatus.STARTING;
        MinecraftServerStatus online = MinecraftServerStatus.ONLINE;
        
        assertNotNull(offline);
        assertNotNull(starting);
        assertNotNull(online);
        
        assertEquals("OFFLINE", offline.name());
        assertEquals("STARTING", starting.name());
        assertEquals("ONLINE", online.name());
    }

    @Test
    public void testCommonServerCreation() {
        CommonServer server = new MockCommonServer("testServer");
        assertNotNull(server);
        assertEquals("testServer", server.getName());
    }

    @Test
    public void testCommonPlayerCreation() {
        CommonPlayer player = new MockCommonPlayer("testPlayer");
        assertNotNull(player);
        assertEquals("testPlayer", player.getUsername());
    }

    @Test
    public void testQueueOperations() {
        List<CommonPlayer> queue = new LinkedList<>();
        CommonPlayer player1 = new MockCommonPlayer("player1");
        CommonPlayer player2 = new MockCommonPlayer("player2");
        
        assertTrue(queue.isEmpty());
        queue.add(player1);
        assertEquals(1, queue.size());
        queue.add(player2);
        assertEquals(2, queue.size());
    }

    @Test
    public void testQueueRemoval() {
        List<CommonPlayer> queue = new LinkedList<>();
        CommonPlayer player1 = new MockCommonPlayer("player1");
        CommonPlayer player2 = new MockCommonPlayer("player2");
        
        queue.add(player1);
        queue.add(player2);
        assertEquals(2, queue.size());
        
        queue.remove(0);
        assertEquals(1, queue.size());
        assertEquals("player2", queue.get(0).getUsername());
    }

    @Test
    public void testQueueContains() {
        List<CommonPlayer> queue = new LinkedList<>();
        CommonPlayer player1 = new MockCommonPlayer("player1");
        CommonPlayer player2 = new MockCommonPlayer("player2");
        
        queue.add(player1);
        assertTrue(queue.contains(player1));
        assertFalse(queue.contains(player2));
    }

    @Test
    public void testQueueClear() {
        List<CommonPlayer> queue = new LinkedList<>();
        queue.add(new MockCommonPlayer("player1"));
        queue.add(new MockCommonPlayer("player2"));
        queue.add(new MockCommonPlayer("player3"));
        assertEquals(3, queue.size());
        
        queue.clear();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    public void testQueueIteration() {
        List<CommonPlayer> queue = new LinkedList<>();
        queue.add(new MockCommonPlayer("player1"));
        queue.add(new MockCommonPlayer("player2"));
        queue.add(new MockCommonPlayer("player3"));
        
        int count = 0;
        for (CommonPlayer player : queue) {
            count++;
            assertNotNull(player.getUsername());
        }
        assertEquals(3, count);
    }

    @Test
    public void testQueueIndexAccess() {
        List<CommonPlayer> queue = new LinkedList<>();
        queue.add(new MockCommonPlayer("player1"));
        queue.add(new MockCommonPlayer("player2"));
        queue.add(new MockCommonPlayer("player3"));
        
        assertEquals("player1", queue.get(0).getUsername());
        assertEquals("player2", queue.get(1).getUsername());
        assertEquals("player3", queue.get(2).getUsername());
    }

    @Test
    public void testQueueRemoveIf() {
        List<CommonPlayer> queue = new LinkedList<>();
        queue.add(new MockCommonPlayer("player1"));
        queue.add(new MockCommonPlayer("player2"));
        queue.add(new MockCommonPlayer("player3"));
        
        queue.removeIf(p -> p.getUsername().equals("player2"));
        assertEquals(2, queue.size());
        assertFalse(queue.stream().anyMatch(p -> p.getUsername().equals("player2")));
    }

    @Test
    public void testMultipleQueues() {
        List<CommonPlayer> queue1 = new LinkedList<>();
        List<CommonPlayer> queue2 = new LinkedList<>();
        
        queue1.add(new MockCommonPlayer("player1"));
        queue2.add(new MockCommonPlayer("player2"));
        
        assertEquals(1, queue1.size());
        assertEquals(1, queue2.size());
        assertNotEquals(queue1.get(0).getUsername(), queue2.get(0).getUsername());
    }

    @Test
    public void testQueueOrder() {
        List<CommonPlayer> queue = new LinkedList<>();
        queue.add(new MockCommonPlayer("first"));
        queue.add(new MockCommonPlayer("second"));
        queue.add(new MockCommonPlayer("third"));
        
        assertEquals("first", queue.get(0).getUsername());
        assertEquals("second", queue.get(1).getUsername());
        assertEquals("third", queue.get(2).getUsername());
    }

    @Test
    public void testServerProperties() {
        CommonServer server = new MockCommonServer("testServer");
        assertNotNull(server);
        assertNotNull(server.getName());
        assertEquals("testServer", server.getName());
    }

    @Test
    public void testPlayerProperties() {
        CommonPlayer player = new MockCommonPlayer("testPlayer");
        assertNotNull(player);
        assertNotNull(player.getUsername());
        assertEquals("testPlayer", player.getUsername());
    }
}

