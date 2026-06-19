package com.lqtigee.sparkai.adapter;

import com.lqtigee.sparkai.dto.AdapterHealthDto;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import java.util.List;

public interface AgentAdapter {

    AgentSource source();

    AdapterHealthDto probe();

    List<RemoteSessionDto> discoverSessions();
}
