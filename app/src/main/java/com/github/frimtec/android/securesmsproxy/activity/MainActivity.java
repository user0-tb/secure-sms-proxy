package com.github.frimtec.android.securesmsproxy.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.frimtec.android.securesmsproxy.R;
import com.github.frimtec.android.securesmsproxy.domain.ApplicationRule;
import com.github.frimtec.android.securesmsproxy.helper.NotificationHelper;
import com.github.frimtec.android.securesmsproxy.helper.Permission;
import com.github.frimtec.android.securesmsproxy.service.ApplicationRuleDao;

import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.github.frimtec.android.securesmsproxy.helper.Permission.SMS;

public class MainActivity extends AppCompatActivity {

  private static final int MENU_CONTEXT_DELETE_ID = 1;
  private ListView listView;

  private ApplicationRuleDao dao;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    if (!SMS.isAllowed(this)) {
      SMS.request(this);
    }

    this.dao = new ApplicationRuleDao();
    SwipeRefreshLayout pullToRefresh = findViewById(R.id.list_pull_to_request);
    pullToRefresh.setOnRefreshListener(() -> {
      refresh();
      pullToRefresh.setRefreshing(false);
    });

    this.listView = findViewById(R.id.list);
    listView.setClickable(true);

    View headerView = getLayoutInflater().inflate(R.layout.application_rule_header, null);
    listView.addHeaderView(headerView);
    registerForContextMenu(listView);
    refresh();
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    menu.add(Menu.NONE, MENU_CONTEXT_DELETE_ID, Menu.NONE, R.string.action_delete);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
    ApplicationRule selectedAlert = (ApplicationRule) listView.getItemAtPosition(info.position);
    switch (item.getItemId()) {
      case MENU_CONTEXT_DELETE_ID:
        NotificationHelper.areYouSure(this, (dialog, which) -> {
          deleteApplicationRule(selectedAlert);
          refresh();
          Toast.makeText(this, R.string.general_entry_deleted, Toast.LENGTH_SHORT).show();
        }, (dialog, which) -> {
        });
        return true;
      default:
        return super.onContextItemSelected(item);
    }
  }

  private void deleteApplicationRule(ApplicationRule applicationRule) {
    dao.delete(applicationRule.getApplication());
  }

  private void refresh() {
    List<ApplicationRule> all = dao.all();
    listView.setAdapter(new ApplicationRuleArrayAdapter(this, all,
        (adapter, applicationRule) -> view -> NotificationHelper.areYouSure(adapter.getContext(), (dialog, which) -> {
          dao.delete(applicationRule.getApplication());
          adapter.remove(applicationRule);
          adapter.notifyDataSetChanged();
          Toast.makeText(adapter.getContext(), R.string.general_entry_deleted, Toast.LENGTH_SHORT).show();
        }, (dialog, which) -> {
        })));
    if (all.isEmpty()) {
      Toast.makeText(this, getString(R.string.general_no_data), Toast.LENGTH_LONG).show();
    }
  }

  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (Permission.RequestCodes.PERMISSION_CHANGED_REQUEST_CODE == requestCode) {
      String text = grantResults[0] == PERMISSION_GRANTED ? getString(R.string.permission_accepted) : getString(R.string.permission_rejected);
      Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.about:
        startActivity(new Intent(this, AboutActivity.class));
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

}
