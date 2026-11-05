package pt.matriz.access.application;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import pt.matriz.common.ApiException;
import pt.matriz.common.CurrentUser;
import pt.matriz.common.CurrentUserProvider;
import pt.matriz.common.ProblemCode;

/** Dá o utilizador atual guardado no pedido pelo filtro de sessão do Acesso. */
@Component
class SessionCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser get() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        Object user = attributes == null
                ? null
                : attributes.getAttribute(CurrentUsers.REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (user instanceof CurrentUser currentUser) {
            return currentUser;
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, ProblemCode.UNAUTHENTICATED, SessionService.SESSION_ENDED);
    }
}
