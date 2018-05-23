package com.yzucse.android.firebasechat;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.common.util.Strings;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class FriendsFragment extends Fragment {
    public FirebaseRecyclerAdapter<User, FriendsViewerHolder>
            mFirebaseAdapter;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private TextView noitemText;
    private GlobalData globalData;
    private String STATUS[];

    public FriendsFragment() {
    }

    public GlobalData getGlobalData() {
        return globalData;
    }

    public void setGlobalData(GlobalData globalData) {
        this.globalData = new GlobalData(globalData);
        //this.globalData = globalData;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFirebaseAdapter != null) mFirebaseAdapter.startListening();
    }

    @Override
    public void onPause() {
        if (mFirebaseAdapter != null) mFirebaseAdapter.stopListening();
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.friend_list_fragment, container, false);

        mProgressBar = view.findViewById(R.id.friendsProgressBar);
        mMessageRecyclerView = view.findViewById(R.id.friendsRecyclerView);
        noitemText = view.findViewById(R.id.noItem);
        noitemText.setVisibility(View.INVISIBLE);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        STATUS = new String[]{getString(R.string.offline), getString(R.string.online)};
        FriendsInit();
        if (mFirebaseAdapter != null) mFirebaseAdapter.startListening();

        return view;
    }

    final private void generateChat(final User friend) {
        Log.e("NEW", "chat");
        ChatRoom friendChat = new ChatRoom();
        friendChat.setChatroomID(globalData.mUser.getUserID() + friend.getUserID());
        friendChat.setChatroomName("chat");
        Map<String, Boolean> savedata = new HashMap<>();
        savedata.put(globalData.mUser.getUserID(), true);
        savedata.put(friend.getUserID(), true);
        friendChat.setUserID(savedata);
        globalData.mChatRoomDBR.child(globalData.mUser.getUserID() + friend.getUserID()).setValue(friendChat);
        globalData.setmChatroom(friendChat);
        globalData.mUser.addChatroom(friendChat.getChatroomID(), friend.getUsername());
        friend.addChatroom(friendChat.getChatroomID(), globalData.mUser.getUsername());
        Map<String, Object> updatecharoom = new HashMap<>();
        updatecharoom.put(StaticValue.CHATROOM, globalData.mUser.getChatrooms());
        globalData.mUsersDBR.child(globalData.mUser.getUserID()).updateChildren(updatecharoom);
        updatecharoom.put(StaticValue.CHATROOM, friend.getChatrooms());
        globalData.mUsersDBR.child(friend.getUserID()).updateChildren(updatecharoom);
    }

    public void FriendsInit() {
        //getActivity().setContentView(R.layout.chat_list);

        // New child entries
        SnapshotParser<User> parser = new SnapshotParser<User>() {
            @Override
            public User parseSnapshot(DataSnapshot dataSnapshot) {
                return dataSnapshot.getValue(User.class);
            }
        };

        Query messagesRef = globalData.mUsersDBR.orderByChild(StaticValue.BLOCKADE + "/" + globalData.mUser.getUserID()).equalTo(false);
        Log.e("TTTTT", messagesRef.toString());

        FirebaseRecyclerOptions<User> options =
                new FirebaseRecyclerOptions.Builder<User>()
                        .setQuery(messagesRef, parser)
                        .build();

        //mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        mFirebaseAdapter = new FirebaseRecyclerAdapter<User, FriendsViewerHolder>(options) {
            @Override
            public FriendsViewerHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new FriendsViewerHolder(inflater.inflate(R.layout.item_friend_list, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(final FriendsViewerHolder viewHolder,
                                            int position,
                                            final User friend) {
                if (friend != null) {
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    noitemText.setVisibility(View.INVISIBLE);
                    globalData.setTextViewText(viewHolder.friendSignView, friend.getSign());
                    globalData.setTextViewText(viewHolder.friendStatusView, STATUS[friend.getOnline() ? 1 : 0]);
                    globalData.setImage(viewHolder.friendImageView, friend.getPhotoUrl(), getActivity());
                    //Log.e(key,muser.getFriends().get(key));
                    //if(fm.containsKey(friend.getUserID()))
                    globalData.setTextViewText(viewHolder.friendNameView,
                            globalData.mUser.getFriends().get(friend.getUserID()));
                    viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final DatabaseReference fchatdbr = globalData.mChatRoomDBR;
                            final String[] key = new String[1];
                            key[0] = "";
                            fchatdbr.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(globalData.mUser.getUserID() + friend.getUserID())) {
                                        key[0] = globalData.mUser.getUserID() + friend.getUserID();
                                        Log.e("AAAA", key[0]);
                                    } else if (dataSnapshot.hasChild(friend.getUserID() + globalData.mUser.getUserID())) {
                                        key[0] = friend.getUserID() + globalData.mUser.getUserID();
                                        Log.e("BBBB", key[0]);
                                    }
                                    if (Strings.isEmptyOrWhitespace(key[0])) {
                                        generateChat(friend);
                                    } else {
                                        fchatdbr.child(key[0]).addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                ChatRoom friendChat = dataSnapshot.getValue(ChatRoom.class);
                                                globalData.setmChatroom(friendChat);
                                                mFirebaseAdapter.stopListening();
                                                Activity thisAct = getActivity();
                                                thisAct.findViewById(R.id.mainLayout).setVisibility(View.INVISIBLE);
                                                thisAct.findViewById(R.id.chatlayout).setVisibility(View.VISIBLE);
                                                ChatFragment mChatFragment = new ChatFragment();
                                                mChatFragment.setGlobalData(globalData);
                                                FragmentManager fragmentManager = getFragmentManager();
                                                FragmentTransaction transaction = fragmentManager.beginTransaction();
                                                transaction.replace(R.id.chatlayout, mChatFragment)
                                                        .commit();
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                    });
                }
            }
        };

        if (mFirebaseAdapter.getItemCount() == 0) {
            noitemText.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            Log.e("ChatRoom", "0");
        }

        /*mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // mUser is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });*/

        mMessageRecyclerView.setAdapter(mFirebaseAdapter);
    }

}
