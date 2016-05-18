/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


function onRequest(context) {
    if (context.request.method == "POST") {
        // TODO: 5/18/16 read HTML form parameters & receive the username & password.
        var username = "admin", password = "admin";
        // TODO: 5/18/16 authenticate username & password.
        try {
            createSession(username);
            sendRedirect(encodeURIComponent(context.app.context + "/"));
        } catch (e) {
            print(e);
        }
    }

}