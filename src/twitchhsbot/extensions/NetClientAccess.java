/*
 */
package twitchhsbot.extensions;

import twitchhsbot.structures.NetClientResponse;

/**
 *
 * @author Toby
 */
public interface NetClientAccess {
    
    public void netResponse(NetClientResponse ncresp);
}
