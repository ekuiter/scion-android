/*
 * Copyright (C) 2019  Vera Clemens, Tom Kranz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.scionlab.endhost;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import io.noties.markwon.Markwon;

public abstract class MarkdownActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_markdown);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getMarkdownResId());
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        Markwon md = Markwon.create(this);
        TextView tv = findViewById(R.id.markdown);
        try(BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open(getString(getMarkdownResId()))))) {
            StringBuilder sb = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                sb.append(line);
                sb.append('\n');
            }
            md.setMarkdown(tv, sb.toString());
            tv.setMovementMethod(LinkMovementMethod.getInstance());
        } catch (IOException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
        }
    }

    abstract int getMarkdownResId();
}
