(ns lasso.components.how-it-works
  "How It Works help section shown to unauthenticated users.")

(defn step
  [number title description]
  [:div.flex.gap-4
   [:div.flex-shrink-0.w-8.h-8.bg-red-500.rounded-full.flex.items-center.justify-center.text-white.font-bold.text-sm
    number]
   [:div
    [:p.font-medium.text-gray-900 title]
    [:p.text-sm.text-gray-600.mt-0.5 description]]])

(defn how-it-works
  "Explainer panel shown below login button for unauthenticated users."
  []
  [:div.bg-white.rounded-lg.shadow.p-6.mt-6
   [:h2.text-lg.font-semibold.text-gray-900.mb-5 "How it works"]
   [:div.space-y-5
    [step "1" "Join a Spotify Jam"
     "Start listening as a guest in someone else's Spotify Jam session."]
    [step "2" "Login with Last.fm"
     "Authenticate with your Last.fm account — no password is stored."]
    [step "3" "Enter the host's Last.fm username"
     "Lasso polls their recent tracks every ~20 seconds."]
    [step "4" "Scrobbles mirror to your account"
     "Any track they scrobble after you start gets added to your Last.fm history too."]]
   [:div.mt-6.pt-5.border-t.border-gray-100
    [:p.text-xs.text-gray-500
     "Only tracks played "
     [:em "after"]
     " you start a session are mirrored — there is no backfill. "
     "The host's Last.fm profile must be public."]]])
