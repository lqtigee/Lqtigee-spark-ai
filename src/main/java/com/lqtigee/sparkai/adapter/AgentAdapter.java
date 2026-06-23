package com.lqtigee.sparkai.adapter;

import com.lqtigee.sparkai.dto.AdapterHealthDto;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import java.util.List;
import java.util.Set;

public interface AgentAdapter {

    AgentSource source();

    AdapterHealthDto probe();

    List<RemoteSessionDto> discoverSessions();

    List<RemoteSessionDto> discoverSessionsByIds(Set<String> ids);
}
