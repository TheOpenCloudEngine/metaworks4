package org.metaworks.iam;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by uengine on 2017. 8. 1..
 * * TODO: should be moved to metaworks4 module
 */
@Data
@AllArgsConstructor
public class SecurityExpressionRoot {
    String loggedUserId;
}
