# Welcome to Cloud Functions for Firebase for Python!
# To get started, simply uncomment the below code or create your own.
# Deploy with `firebase deploy`

from typing import Any

from firebase_functions import https_fn
from firebase_functions.options import set_global_options
from firebase_admin import initialize_app

# For cost control, you can set the maximum number of containers that can be
# running at the same time. This helps mitigate the impact of unexpected
# traffic spikes by instead downgrading performance. This limit is a per-function
# limit. You can override the limit for each function using the max_instances
# parameter in the decorator, e.g. @https_fn.on_request(max_instances=5).
set_global_options(max_instances=1)

initialize_app()

@https_fn.on_call()
def on_call_example(req: https_fn.CallableRequest) -> Any:
    return {"message": "Hello world!"}  # Return dict directly, no JSON conversion needed

