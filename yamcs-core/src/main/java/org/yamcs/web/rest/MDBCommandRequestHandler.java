package org.yamcs.web.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.protobuf.Mdb.CommandInfo;
import org.yamcs.protobuf.Rest.ListCommandsResponse;
import org.yamcs.protobuf.SchemaMdb;
import org.yamcs.protobuf.SchemaRest;
import org.yamcs.protobuf.Yamcs.NamedObjectId;
import org.yamcs.security.Privilege;
import org.yamcs.security.Privilege.Type;
import org.yamcs.web.rest.RestUtils.MatchResult;
import org.yamcs.web.rest.XtceToGpbAssembler.DetailLevel;
import org.yamcs.xtce.MetaCommand;
import org.yamcs.xtce.XtceDb;

/**
 * Handles incoming requests related to command info from the MDB
 */
public class MDBCommandRequestHandler extends RestRequestHandler {
    final static Logger log = LoggerFactory.getLogger(MDBCommandRequestHandler.class.getName());
    
    @Override
    public String getPath() {
        return "commands";
    }
    
    @Override
    public RestResponse handleRequest(RestRequest req, int pathOffset) throws RestException {
        XtceDb mdb = req.getFromContext(MDBRequestHandler.CTX_MDB);
        if (!req.hasPathSegment(pathOffset)) {
            return listCommands(req, null, mdb); // root namespace
        } else {
            MatchResult<MetaCommand> c = RestUtils.matchCommandName(req, pathOffset);
            if (c.matches()) { // command
                return getSingleCommand(req, c.getRequestedId(), c.getMatch());
            } else { // namespace
                return listCommandsOrError(req, pathOffset);
            }
        }
    }
    
    private RestResponse listCommandsOrError(RestRequest req, int pathOffset) throws RestException {
        XtceDb mdb = req.getFromContext(MDBRequestHandler.CTX_MDB);
        MatchResult<String> nsm = RestUtils.matchXtceDbNamespace(req, pathOffset);
        if (nsm.matches()) {
            return listCommands(req, nsm.getMatch(), mdb);
        } else {
            throw new NotFoundException(req);
        }
    }
    
    private RestResponse getSingleCommand(RestRequest req, NamedObjectId id, MetaCommand cmd) throws RestException {
        if (!Privilege.getInstance().hasPrivilege(req.authToken, Privilege.Type.TC, cmd.getQualifiedName())) {
            log.warn("Command Info for {} not authorized for token {}, throwing BadRequestException", id, req.authToken);
            throw new BadRequestException("Invalid command name specified "+id);
        }
        String instanceURL = req.getApiURL() + "/mdb/" + req.getFromContext(RestRequest.CTX_INSTANCE);
        CommandInfo cinfo = XtceToGpbAssembler.toCommandInfo(cmd, instanceURL, DetailLevel.FULL);
        return new RestResponse(req, cinfo, SchemaMdb.CommandInfo.WRITE);
    }

    /**
     * Sends the commands for the requested yamcs instance. If no namespace
     * is specified, assumes root namespace.
     */
    private RestResponse listCommands(RestRequest req, String namespace, XtceDb mdb) throws RestException {
        String instanceURL = req.getApiURL() + "/mdb/" + req.getFromContext(RestRequest.CTX_INSTANCE);
        
        NameDescriptionSearchMatcher matcher = null;
        if (req.hasQueryParameter("q")) {
            matcher = new NameDescriptionSearchMatcher(req.getQueryParameter("q"));    
        }
        
        ListCommandsResponse.Builder responseb = ListCommandsResponse.newBuilder();
        if (namespace == null) {
            for (MetaCommand cmd : mdb.getMetaCommands()) {
                if (matcher != null && !matcher.matches(cmd)) continue;
                responseb.addCommand(XtceToGpbAssembler.toCommandInfo(cmd, instanceURL, DetailLevel.SUMMARY));
            }
        } else {
            Privilege privilege = Privilege.getInstance();
            for (MetaCommand cmd : mdb.getMetaCommands()) {
                if (!privilege.hasPrivilege(req.authToken, Type.TC, cmd.getQualifiedName()))
                    continue;
                if (matcher != null && !matcher.matches(cmd))
                    continue;
                
                String alias = cmd.getAlias(namespace);
                if (alias != null) {
                    responseb.addCommand(XtceToGpbAssembler.toCommandInfo(cmd, instanceURL, DetailLevel.SUMMARY));
                }
            }
        }
        
        return new RestResponse(req, responseb.build(), SchemaRest.ListCommandsResponse.WRITE);
    }
}