from opendc.models.model import Model
from opendc.models.user import User
from opendc.util.rest import Response


class Simulation(Model):
    collection_name = 'simulations'

    def validate_user_access(self, google_id, edit_access):
        user = User.from_google_id(google_id)
        authorizations = list(
            filter(lambda x: str(x['simulationId']) == str(self.obj['_id']), user.obj['authorizations']))
        if len(authorizations) == 0 or (edit_access and authorizations[0]['authorizationLevel'] == 'VIEW'):
            return Response(403, "Forbidden from retrieving simulation.")

        return None
