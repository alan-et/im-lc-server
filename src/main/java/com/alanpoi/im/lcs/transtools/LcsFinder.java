package com.alanpoi.im.lcs.transtools;

import com.qzd.im.common.model.PersonId;
import com.alanpoi.im.lcs.transtools.network.TcpClient;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author brandon
 * @create 2019-07-18
 *
 * LCS服务finder类型
 */
public interface LcsFinder {

    Map<PersonId, Set<String>> getOnlinePersons(List<PersonId> personIds);

    TcpClient getLcsClient(String lcsId);

    LcsInfo getLcsInfo(String lcsId);

    void lcsClientInvalid(String lcsId, TcpClient client);
}
